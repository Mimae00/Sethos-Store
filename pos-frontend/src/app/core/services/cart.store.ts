import { Injectable, computed, signal } from '@angular/core';
import { Product } from '../models/catalogue.model';
import { CheckoutPayload, PaymentMethod } from '../models/sale.model';
import { clampToZero, percentageOf, round2 } from '../util/money';

export interface CartLine {
  productId: number;
  sku: string;
  name: string;
  unit: string;
  /** Effective price: the product's price unless the cashier overrode it. */
  unitPrice: number;
  /** Catalogue price, kept so an override can be shown and undone. */
  listPrice: number;
  taxRate: number;
  quantity: number;
  discountAmount: number;
  /** Null when the product is a service and stock is not tracked. */
  stockAvailable: number | null;
}

export interface CartLineTotals {
  gross: number;
  discount: number;
  net: number;
  tax: number;
  lineTotal: number;
}

/**
 * The open basket.
 *
 * <p>Totals are computed here purely so the cashier sees them change instantly. The server
 * recalculates everything on checkout and its numbers are what get stored; if the two ever
 * disagree the receipt wins. {@code core/util/money} keeps the arithmetic identical.</p>
 */
@Injectable({ providedIn: 'root' })
export class CartStore {
  private readonly items = signal<CartLine[]>([]);
  private readonly discount = signal(0);
  private readonly cashier = signal('');
  private readonly customer = signal<number | null>(null);
  private readonly saleNote = signal('');

  readonly lines = this.items.asReadonly();
  readonly orderDiscount = this.discount.asReadonly();
  readonly cashierName = this.cashier.asReadonly();
  readonly customerId = this.customer.asReadonly();
  readonly note = this.saleNote.asReadonly();

  readonly isEmpty = computed(() => this.items().length === 0);
  readonly lineCount = computed(() => this.items().length);
  readonly unitCount = computed(() => this.items().reduce((sum, line) => sum + line.quantity, 0));

  /** Per-line breakdown, keyed by product id, in the same order as {@link lines}. */
  readonly lineTotals = computed<Map<number, CartLineTotals>>(() => {
    const totals = new Map<number, CartLineTotals>();
    for (const line of this.items()) {
      totals.set(line.productId, CartStore.totalsFor(line));
    }
    return totals;
  });

  /** Sum of line amounts at the charged price, before discounts and tax. */
  readonly subtotal = computed(() =>
    round2(this.sum((totals) => totals.gross)),
  );

  readonly lineDiscountTotal = computed(() =>
    round2(this.sum((totals) => totals.discount)),
  );

  readonly taxTotal = computed(() => round2(this.sum((totals) => totals.tax)));

  /** Everything owed before the order-level discount is taken off. */
  readonly taxedTotal = computed(() => round2(this.sum((totals) => totals.lineTotal)));

  /** Line discounts plus the order discount, matching the receipt's discountTotal. */
  readonly discountTotal = computed(() =>
    round2(this.lineDiscountTotal() + this.effectiveOrderDiscount()),
  );

  /** Amount due. Never negative, and never more than the basket is worth. */
  readonly total = computed(() =>
    clampToZero(round2(this.taxedTotal() - this.effectiveOrderDiscount())),
  );

  /** The order discount actually applied, capped at the taxed total. */
  readonly effectiveOrderDiscount = computed(() =>
    round2(Math.min(Math.max(this.discount(), 0), this.taxedTotal())),
  );

  /**
   * Adds a product, or bumps the quantity if it is already in the basket.
   *
   * @returns false when stock would be exceeded, so the caller can warn instead of
   *          silently adding a line that checkout will reject.
   */
  add(product: Product, quantity = 1): boolean {
    if (quantity <= 0) {
      return false;
    }
    const existing = this.items().find((line) => line.productId === product.id);
    const requested = (existing?.quantity ?? 0) + quantity;

    if (product.trackStock && requested > product.stockQuantity) {
      return false;
    }

    if (existing) {
      this.setQuantity(product.id, requested);
      return true;
    }

    this.items.update((lines) => [
      ...lines,
      {
        productId: product.id,
        sku: product.sku,
        name: product.name,
        unit: product.unit,
        unitPrice: product.price,
        listPrice: product.price,
        taxRate: product.taxRate,
        quantity,
        discountAmount: 0,
        stockAvailable: product.trackStock ? product.stockQuantity : null,
      },
    ]);
    return true;
  }

  /** Sets an absolute quantity. Zero or less removes the line. */
  setQuantity(productId: number, quantity: number): boolean {
    if (quantity <= 0) {
      this.remove(productId);
      return true;
    }
    const line = this.items().find((candidate) => candidate.productId === productId);
    if (!line) {
      return false;
    }
    if (line.stockAvailable !== null && quantity > line.stockAvailable) {
      return false;
    }
    this.patch(productId, { quantity: Math.floor(quantity) });
    return true;
  }

  increment(productId: number): boolean {
    const line = this.items().find((candidate) => candidate.productId === productId);
    return line ? this.setQuantity(productId, line.quantity + 1) : false;
  }

  decrement(productId: number): void {
    const line = this.items().find((candidate) => candidate.productId === productId);
    if (line) {
      this.setQuantity(productId, line.quantity - 1);
    }
  }

  /** Manager override on the selling price. */
  setUnitPrice(productId: number, unitPrice: number): void {
    this.patch(productId, { unitPrice: round2(Math.max(unitPrice, 0)) });
  }

  /** Absolute amount off the line, capped at the line's own value. */
  setLineDiscount(productId: number, discountAmount: number): void {
    const line = this.items().find((candidate) => candidate.productId === productId);
    if (!line) {
      return;
    }
    const gross = round2(line.unitPrice * line.quantity);
    this.patch(productId, { discountAmount: round2(Math.min(Math.max(discountAmount, 0), gross)) });
  }

  setOrderDiscount(amount: number): void {
    this.discount.set(round2(Math.max(amount, 0)));
  }

  setCashierName(name: string): void {
    this.cashier.set(name);
  }

  setCustomerId(customerId: number | null): void {
    this.customer.set(customerId);
  }

  setNote(note: string): void {
    this.saleNote.set(note);
  }

  remove(productId: number): void {
    this.items.update((lines) => lines.filter((line) => line.productId !== productId));
  }

  /** Empties the basket but keeps the cashier name for the next customer. */
  clear(): void {
    this.items.set([]);
    this.discount.set(0);
    this.customer.set(null);
    this.saleNote.set('');
  }

  /**
   * Builds the checkout request.
   *
   * Sends the overridden price only when it differs from the catalogue price, so the
   * server uses its own current price in the normal case.
   */
  toPayload(paymentMethod: PaymentMethod, amountTendered: number): CheckoutPayload {
    return {
      items: this.items().map((line) => ({
        productId: line.productId,
        quantity: line.quantity,
        unitPrice: line.unitPrice === line.listPrice ? null : line.unitPrice,
        discountAmount: line.discountAmount || null,
      })),
      customerId: this.customer(),
      paymentMethod,
      amountTendered: round2(amountTendered),
      orderDiscount: this.effectiveOrderDiscount() || null,
      cashierName: this.cashier().trim() || null,
      note: this.saleNote().trim() || null,
    };
  }

  static totalsFor(line: CartLine): CartLineTotals {
    const gross = round2(line.unitPrice * line.quantity);
    const discount = round2(Math.min(line.discountAmount, gross));
    const net = round2(gross - discount);
    const tax = percentageOf(net, line.taxRate);
    return { gross, discount, net, tax, lineTotal: round2(net + tax) };
  }

  private sum(pick: (totals: CartLineTotals) => number): number {
    return this.items().reduce((accumulator, line) => accumulator + pick(CartStore.totalsFor(line)), 0);
  }

  private patch(productId: number, changes: Partial<CartLine>): void {
    this.items.update((lines) =>
      lines.map((line) => (line.productId === productId ? { ...line, ...changes } : line)),
    );
  }
}
