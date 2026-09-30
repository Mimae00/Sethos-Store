import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { API_BASE_URL } from '../api/api.tokens';
import { toHttpParams } from '../api/http-params';
import {
  DailySalesPoint,
  Dashboard,
  PaymentMethodTotal,
  SalesSummary,
  TopProduct,
} from '../models/report.model';

@Injectable({ providedIn: 'root' })
export class ReportService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${inject(API_BASE_URL)}/reports`;

  /** Everything the landing screen needs, in one request. */
  dashboard(): Observable<Dashboard> {
    return this.http.get<Dashboard>(`${this.baseUrl}/dashboard`);
  }

  summary(from?: string, to?: string): Observable<SalesSummary> {
    return this.http.get<SalesSummary>(`${this.baseUrl}/summary`, {
      params: toHttpParams({ from, to }),
    });
  }

  dailySales(from?: string, to?: string): Observable<DailySalesPoint[]> {
    return this.http.get<DailySalesPoint[]>(`${this.baseUrl}/daily-sales`, {
      params: toHttpParams({ from, to }),
    });
  }

  topProducts(from?: string, to?: string, limit = 10): Observable<TopProduct[]> {
    return this.http.get<TopProduct[]>(`${this.baseUrl}/top-products`, {
      params: toHttpParams({ from, to, limit }),
    });
  }

  paymentMethods(from?: string, to?: string): Observable<PaymentMethodTotal[]> {
    return this.http.get<PaymentMethodTotal[]>(`${this.baseUrl}/payment-methods`, {
      params: toHttpParams({ from, to }),
    });
  }
}
