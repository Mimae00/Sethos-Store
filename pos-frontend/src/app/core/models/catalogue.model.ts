export interface Category {
  id: number;
  name: string;
  description: string | null;
  color: string | null;
  displayOrder: number;
}

export interface CategoryPayload {
  name: string;
  description?: string | null;
  color?: string | null;
  displayOrder?: number | null;
}

export interface Product {
  id: number;
  sku: string;
  barcode: string | null;
  name: string;
  description: string | null;
  price: number;
  cost: number;
  /** Percentage, e.g. 10 means 10%. */
  taxRate: number;
  stockQuantity: number;
  reorderLevel: number;
  unit: string;
  imageUrl: string | null;
  active: boolean;
  trackStock: boolean;
  /** Computed by the backend: stock has reached the reorder level. */
  lowStock: boolean;
  category: Category | null;
  createdAt: string;
  updatedAt: string;
}

export interface ProductPayload {
  sku: string;
  barcode?: string | null;
  name: string;
  description?: string | null;
  price: number;
  cost?: number | null;
  taxRate?: number | null;
  /** Only honoured on create, as opening stock. Ignored on update. */
  stockQuantity?: number | null;
  reorderLevel?: number | null;
  unit?: string | null;
  imageUrl?: string | null;
  active?: boolean | null;
  trackStock?: boolean | null;
  categoryId?: number | null;
}

export interface ProductQuery {
  search?: string;
  categoryId?: number | null;
  active?: boolean | null;
  lowStock?: boolean | null;
  page?: number;
  size?: number;
  sort?: string;
}

export type StockMovementType = 'PURCHASE' | 'ADJUSTMENT' | 'SALE' | 'RETURN' | 'SHRINKAGE';

export interface StockAdjustmentPayload {
  /** Signed delta. Negative reduces stock. */
  quantityChange: number;
  type: StockMovementType;
  reason?: string | null;
}

export interface StockMovement {
  id: number;
  productId: number;
  productName: string;
  productSku: string;
  type: StockMovementType;
  quantityChange: number;
  quantityBefore: number;
  quantityAfter: number;
  reference: string | null;
  reason: string | null;
  createdAt: string;
}

export interface Customer {
  id: number;
  name: string;
  phone: string | null;
  email: string | null;
  address: string | null;
  notes: string | null;
  createdAt: string;
}

export interface CustomerPayload {
  name: string;
  phone?: string | null;
  email?: string | null;
  address?: string | null;
  notes?: string | null;
}
