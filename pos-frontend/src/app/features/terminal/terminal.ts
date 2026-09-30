import { Component, computed, effect, inject, signal, untracked } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Category, Customer, Product } from '../../core/models/catalogue.model';
import { PAYMENT_METHODS, PaymentMethod, Sale } from '../../core/models/sale.model';
import { CartStore } from '../../core/services/cart.store';
import { CategoryService } from '../../core/services/category.service';
import { CustomerService } from '../../core/services/customer.service';
import { ProductService } from '../../core/services/product.service';
import { SaleService } from '../../core/services/sale.service';
import { SettingsStore } from '../../core/services/settings.store';
import { ToastService } from '../../core/services/toast.service';
import { parseAmount, round2 } from '../../core/util/money';
import { MoneyPipe } from '../../shared/money.pipe';
import { Receipt } from '../../shared/receipt';

/**
 * The till.
 *
 * <p>Left half is the catalogue (search, barcode, category filter), right half is the
 * open basket and payment. The basket lives in {@link CartStore} rather than here so a
 * cashier can check a price on another screen and come back without losing the sale.</p>
 */
@Component({
  selector: 'app-terminal',
  imports: [FormsModule, MoneyPipe, Receipt],
  templateUrl: './terminal.html',
  styleUrl: './terminal.css',
})
export class Terminal {
  private readonly productService = inject(ProductService);
  private readonly categoryService = inject(CategoryService);
  private readonly customerService = inject(CustomerService);
  private readonly saleService = inject(SaleService);
  private readonly toasts = inject(ToastService);
  private readonly settingsStore = inject(SettingsStore);

  protected readonly cart = inject(CartStore);
  protected readonly paymentMethods = PAYMENT_METHODS;
  protected readonly settings = this.settingsStore.settings;

  protected readonly categories = signal<Category[]>([]);
  protected readonly products = signal<Product[]>([]);
  protected readonly customers = signal<Customer[]>([]);
  protected readonly loadingProducts = signal(false);
  protected readonly submitting = signal(false);

  protected readonly searchTerm = signal('');
  protected readonly activeCategoryId = signal<number | null>(null);
  protected readonly barcode = signal('');

  protected readonly paymentMethod = signal<PaymentMethod>('CASH');
  protected readonly tendered = signal('');

  /** Set after a successful checkout so the receipt can be shown and printed. */
  protected readonly completedSale = signal<Sale | null>(null);

  private searchTimer: ReturnType<typeof setTimeout> | null = null;

  constructor() {
    this.loadCategories();
    this.loadCustomers();
    this.loadProducts();

    // Cash is the only method that needs a tendered amount; clearing the field when
    // switching away stops a stale value being sent with a card sale.
    effect(() => {
      const method = this.paymentMethod();
      untracked(() => {
        if (method !== 'CASH') {
          this.tendered.set('');
        }
      });
    });
  }

  protected readonly tenderedAmount = computed(() => round2(parseAmount(this.tendered())));

  protected readonly isCash = computed(() => this.paymentMethod() === 'CASH');

  /** Negative until enough cash has been entered. */
  protected readonly changeDue = computed(() =>
    round2(this.tenderedAmount() - this.cart.total()),
  );

  protected readonly canCheckout = computed(() => {
    if (this.cart.isEmpty() || this.submitting()) {
      return false;
    }
    return !this.isCash() || this.tenderedAmount() >= this.cart.total();
  });

  /**
   * Handy tender buttons: the exact amount, then the next few round figures above it.
   * Saves the cashier typing the common cases.
   */
  protected readonly quickTenders = computed(() => {
    const total = this.cart.total();
    if (total <= 0) {
      return [];
    }
    const steps = [5, 10, 20, 50, 100, 200, 500, 1000];
    const suggestions = new Set<number>([total]);
    for (const step of steps) {
      const rounded = Math.ceil(total / step) * step;
      if (rounded > total) {
        suggestions.add(rounded);
      }
      if (suggestions.size >= 5) {
        break;
      }
    }
    return [...suggestions].sort((a, b) => a - b).slice(0, 5);
  });

  protected readonly visibleProducts = computed(() => this.products());

  protected lineTotals(productId: number) {
    return this.cart.lineTotals().get(productId);
  }

  // --- Catalogue ---------------------------------------------------------

  protected onSearchInput(value: string): void {
    this.searchTerm.set(value);
    if (this.searchTimer) {
      clearTimeout(this.searchTimer);
    }
    // Debounced so typing a product name does not fire a request per keystroke.
    this.searchTimer = setTimeout(() => this.loadProducts(), 250);
  }

  protected selectCategory(categoryId: number | null): void {
    this.activeCategoryId.set(categoryId);
    this.loadProducts();
  }

  protected loadProducts(): void {
    this.loadingProducts.set(true);
    this.productService
      .search({
        search: this.searchTerm().trim(),
        categoryId: this.activeCategoryId(),
        // Only sellable items belong on the till.
        active: true,
        size: 100,
        sort: 'name,asc',
      })
      .subscribe({
        next: (page) => {
          this.products.set(page.content);
          this.loadingProducts.set(false);
        },
        error: (error: unknown) => {
          this.loadingProducts.set(false);
          this.toasts.fromError(error, 'Could not load products');
        },
      });
  }

  private loadCategories(): void {
    this.categoryService.list().subscribe({
      next: (categories) => this.categories.set(categories),
      error: (error: unknown) => this.toasts.fromError(error, 'Could not load categories'),
    });
  }

  private loadCustomers(): void {
    this.customerService.search('', 0, 200).subscribe({
      next: (page) => this.customers.set(page.content),
      error: () => {
        // Customers are optional on a sale; a failure here must not block selling.
      },
    });
  }

  // --- Basket ------------------------------------------------------------

  protected addToCart(product: Product): void {
    if (!this.cart.add(product)) {
      this.toasts.error(
        `${product.name} has only ${product.stockQuantity} ${product.unit} in stock`,
      );
    }
  }

  /** Barcode scanners type the code then press Enter. */
  protected onBarcodeSubmit(): void {
    const code = this.barcode().trim();
    if (!code) {
      return;
    }
    this.productService.findByBarcode(code).subscribe({
      next: (product) => {
        this.barcode.set('');
        if (!product.active) {
          this.toasts.error(`${product.name} is not currently for sale`);
          return;
        }
        this.addToCart(product);
      },
      error: () => this.toasts.error(`No product with barcode ${code}`),
    });
  }

  protected changeQuantity(productId: number, value: string): void {
    const quantity = Math.floor(parseAmount(value));
    if (!this.cart.setQuantity(productId, quantity)) {
      const line = this.cart.lines().find((candidate) => candidate.productId === productId);
      this.toasts.error(`Only ${line?.stockAvailable ?? 0} in stock`);
    }
  }

  protected increment(productId: number): void {
    if (!this.cart.increment(productId)) {
      const line = this.cart.lines().find((candidate) => candidate.productId === productId);
      this.toasts.error(`Only ${line?.stockAvailable ?? 0} in stock`);
    }
  }

  protected changeLineDiscount(productId: number, value: string): void {
    this.cart.setLineDiscount(productId, parseAmount(value));
  }

  protected changeUnitPrice(productId: number, value: string): void {
    this.cart.setUnitPrice(productId, parseAmount(value));
  }

  protected changeOrderDiscount(value: string): void {
    this.cart.setOrderDiscount(parseAmount(value));
  }

  protected changeCustomer(value: string): void {
    this.cart.setCustomerId(value ? Number(value) : null);
  }

  protected applyQuickTender(amount: number): void {
    this.tendered.set(amount.toFixed(2));
  }

  protected clearBasket(): void {
    if (this.cart.isEmpty()) {
      return;
    }
    this.cart.clear();
    this.tendered.set('');
    this.toasts.info('Basket cleared');
  }

  // --- Checkout ----------------------------------------------------------

  protected checkout(): void {
    if (!this.canCheckout()) {
      return;
    }
    this.submitting.set(true);

    // Non-cash sales settle for the exact amount; the server enforces this too.
    const tenderedForSale = this.isCash() ? this.tenderedAmount() : this.cart.total();

    this.saleService.checkout(this.cart.toPayload(this.paymentMethod(), tenderedForSale)).subscribe({
      next: (sale) => {
        this.submitting.set(false);
        this.completedSale.set(sale);
        this.cart.clear();
        this.tendered.set('');
        this.toasts.success(`Sale ${sale.reference} completed`);
        // Stock changed, so the tiles need refreshing.
        this.loadProducts();
      },
      error: (error: unknown) => {
        this.submitting.set(false);
        this.toasts.fromError(error, 'Checkout failed');
      },
    });
  }

  protected closeReceipt(): void {
    this.completedSale.set(null);
    document.body.classList.remove('printing-receipt');
  }

  protected printReceipt(): void {
    // The print stylesheet keys off this class to strip the app chrome.
    document.body.classList.add('printing-receipt');
    window.print();
    document.body.classList.remove('printing-receipt');
  }

  protected stockBadge(product: Product): string {
    if (!product.trackStock) {
      return 'badge--info';
    }
    if (product.stockQuantity <= 0) {
      return 'badge--danger';
    }
    return product.lowStock ? 'badge--warning' : 'badge--neutral';
  }

  protected stockLabel(product: Product): string {
    if (!product.trackStock) {
      return 'Service';
    }
    if (product.stockQuantity <= 0) {
      return 'Out of stock';
    }
    return `${product.stockQuantity} ${product.unit}`;
  }
}
