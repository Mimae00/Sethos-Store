import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { toHttpParams } from '../api/http-params';
import { API_BASE_URL } from '../api/api.tokens';
import {
  Product,
  ProductPayload,
  ProductQuery,
  StockAdjustmentPayload,
  StockMovement,
} from '../models/catalogue.model';
import { Page } from '../models/common.model';

@Injectable({ providedIn: 'root' })
export class ProductService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${inject(API_BASE_URL)}/products`;

  search(query: ProductQuery = {}): Observable<Page<Product>> {
    return this.http.get<Page<Product>>(this.baseUrl, {
      params: toHttpParams({
        search: query.search,
        categoryId: query.categoryId,
        active: query.active,
        lowStock: query.lowStock,
        page: query.page ?? 0,
        size: query.size ?? 20,
        sort: query.sort ?? 'name,asc',
      }),
    });
  }

  get(id: number): Observable<Product> {
    return this.http.get<Product>(`${this.baseUrl}/${id}`);
  }

  /** Barcode scan path. Returns 404 when nothing matches. */
  findByBarcode(barcode: string): Observable<Product> {
    return this.http.get<Product>(`${this.baseUrl}/by-barcode/${encodeURIComponent(barcode)}`);
  }

  findBySku(sku: string): Observable<Product> {
    return this.http.get<Product>(`${this.baseUrl}/by-sku/${encodeURIComponent(sku)}`);
  }

  lowStock(limit = 20): Observable<Product[]> {
    return this.http.get<Product[]>(`${this.baseUrl}/low-stock`, {
      params: toHttpParams({ limit }),
    });
  }

  create(payload: ProductPayload): Observable<Product> {
    return this.http.post<Product>(this.baseUrl, payload);
  }

  update(id: number, payload: ProductPayload): Observable<Product> {
    return this.http.put<Product>(`${this.baseUrl}/${id}`, payload);
  }

  /** Deactivates the product if it appears in past sales, otherwise deletes it. */
  remove(id: number): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${id}`);
  }

  adjustStock(id: number, payload: StockAdjustmentPayload): Observable<Product> {
    return this.http.post<Product>(`${this.baseUrl}/${id}/stock-adjustments`, payload);
  }

  movements(id: number, page = 0, size = 20): Observable<Page<StockMovement>> {
    return this.http.get<Page<StockMovement>>(`${this.baseUrl}/${id}/stock-movements`, {
      params: toHttpParams({ page, size }),
    });
  }
}
