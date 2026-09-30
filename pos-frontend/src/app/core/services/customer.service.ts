import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { API_BASE_URL } from '../api/api.tokens';
import { toHttpParams } from '../api/http-params';
import { Customer, CustomerPayload } from '../models/catalogue.model';
import { Page } from '../models/common.model';

@Injectable({ providedIn: 'root' })
export class CustomerService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${inject(API_BASE_URL)}/customers`;

  search(search = '', page = 0, size = 20): Observable<Page<Customer>> {
    return this.http.get<Page<Customer>>(this.baseUrl, {
      params: toHttpParams({ search, page, size }),
    });
  }

  create(payload: CustomerPayload): Observable<Customer> {
    return this.http.post<Customer>(this.baseUrl, payload);
  }

  update(id: number, payload: CustomerPayload): Observable<Customer> {
    return this.http.put<Customer>(`${this.baseUrl}/${id}`, payload);
  }

  remove(id: number): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${id}`);
  }
}
