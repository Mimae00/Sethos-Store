import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { API_BASE_URL } from '../api/api.tokens';
import { toHttpParams } from '../api/http-params';
import { Page } from '../models/common.model';
import { CheckoutPayload, Sale, SaleQuery, SaleSummary } from '../models/sale.model';

@Injectable({ providedIn: 'root' })
export class SaleService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${inject(API_BASE_URL)}/sales`;

  search(query: SaleQuery = {}): Observable<Page<SaleSummary>> {
    return this.http.get<Page<SaleSummary>>(this.baseUrl, {
      params: toHttpParams({
        search: query.search,
        status: query.status,
        from: query.from,
        to: query.to,
        page: query.page ?? 0,
        size: query.size ?? 20,
      }),
    });
  }

  get(id: number): Observable<Sale> {
    return this.http.get<Sale>(`${this.baseUrl}/${id}`);
  }

  findByReference(reference: string): Observable<Sale> {
    return this.http.get<Sale>(`${this.baseUrl}/by-reference/${encodeURIComponent(reference)}`);
  }

  /** Rings up a basket. Returns the full receipt, including the change due. */
  checkout(payload: CheckoutPayload): Observable<Sale> {
    return this.http.post<Sale>(this.baseUrl, payload);
  }

  /** Cancels a completed sale and returns the stock. */
  void(id: number, reason: string): Observable<Sale> {
    return this.http.post<Sale>(`${this.baseUrl}/${id}/void`, { reason });
  }
}
