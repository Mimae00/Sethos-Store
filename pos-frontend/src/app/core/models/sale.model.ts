import { Customer } from './catalogue.model';

export type PaymentMethod = 'CASH' | 'CARD' | 'EWALLET' | 'BANK_TRANSFER' | 'OTHER';
export type SaleStatus = 'COMPLETED' | 'VOIDED' | 'REFUNDED';

export const PAYMENT_METHODS: readonly { value: PaymentMethod; label: string }[] = [
  { value: 'CASH', label: 'Cash' },
  { value: 'CARD', label: 'Card' },
  { value: 'EWALLET', label: 'E-wallet' },
  { value: 'BANK_TRANSFER', label: 'Bank transfer' },
  { value: 'OTHER', label: 'Other' },
];

export interface SaleItem {
  id: number;
  productId: number | null;
  productName: string;
  productSku: string;
  unit: string;
  unitPrice: number;
  quantity: number;
  grossAmount: number;
  discountAmount: number;
  netAmount: number;
  taxRate: number;
  taxAmount: number;
  lineTotal: number;
}

export interface Sale {
  id: number;
  reference: string;
  soldAt: string;
  status: SaleStatus;
  paymentMethod: PaymentMethod;
  subtotal: number;
  discountTotal: number;
  orderDiscount: number;
  taxTotal: number;
  total: number;
  amountTendered: number;
  changeDue: number;
  cashierName: string;
  note: string | null;
  voidReason: string | null;
  voidedAt: string | null;
  customer: Customer | null;
  totalUnits: number;
  items: SaleItem[];
}

/** History row: no line items, so the list endpoint stays cheap. */
export interface SaleSummary {
  id: number;
  reference: string;
  soldAt: string;
  status: SaleStatus;
  paymentMethod: PaymentMethod;
  total: number;
  taxTotal: number;
  discountTotal: number;
  cashierName: string;
  customerName: string | null;
}

export interface CheckoutItemPayload {
  productId: number;
  quantity: number;
  /** Manager override. Omit to use the product's current price. */
  unitPrice?: number | null;
  /** Absolute amount off this line, not a percentage. */
  discountAmount?: number | null;
}

export interface CheckoutPayload {
  items: CheckoutItemPayload[];
  customerId?: number | null;
  paymentMethod: PaymentMethod;
  amountTendered?: number | null;
  orderDiscount?: number | null;
  cashierName?: string | null;
  note?: string | null;
}

export interface SaleQuery {
  search?: string;
  status?: SaleStatus | null;
  /** Inclusive ISO date (yyyy-MM-dd) in the store's time zone. */
  from?: string | null;
  to?: string | null;
  page?: number;
  size?: number;
}
