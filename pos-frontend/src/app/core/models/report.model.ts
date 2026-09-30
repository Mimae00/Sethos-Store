import { Product } from './catalogue.model';
import { PaymentMethod } from './sale.model';

/**
 * Money fields form an accounting ladder:
 * grossSales - discountTotal = netSales, + taxTotal = totalCollected.
 */
export interface SalesSummary {
  from: string;
  to: string;
  saleCount: number;
  unitsSold: number;
  grossSales: number;
  discountTotal: number;
  netSales: number;
  taxTotal: number;
  totalCollected: number;
  costOfGoodsSold: number;
  grossProfit: number;
  averageSaleValue: number;
}

export interface DailySalesPoint {
  date: string;
  saleCount: number;
  total: number;
}

export interface TopProduct {
  productId: number | null;
  sku: string;
  name: string;
  quantitySold: number;
  revenue: number;
}

export interface PaymentMethodTotal {
  paymentMethod: PaymentMethod;
  saleCount: number;
  total: number;
}

export interface Dashboard {
  today: SalesSummary;
  last7Days: SalesSummary;
  thisMonth: SalesSummary;
  activeProducts: number;
  lowStockCount: number;
  inventoryCostValue: number;
  dailySales: DailySalesPoint[];
  topProducts: TopProduct[];
  paymentBreakdown: PaymentMethodTotal[];
  lowStockProducts: Product[];
}

export interface StoreSettings {
  storeName: string;
  storeAddress: string;
  storePhone: string;
  taxIdentifier: string;
  currencyCode: string;
  currencySymbol: string;
  timeZone: string;
  defaultTaxRate: number;
  receiptFooter: string;
}
