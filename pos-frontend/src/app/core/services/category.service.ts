import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { API_BASE_URL } from '../api/api.tokens';
import { Category, CategoryPayload } from '../models/catalogue.model';

@Injectable({ providedIn: 'root' })
export class CategoryService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${inject(API_BASE_URL)}/categories`;

  list(): Observable<Category[]> {
    return this.http.get<Category[]>(this.baseUrl);
  }

  get(id: number): Observable<Category> {
    return this.http.get<Category>(`${this.baseUrl}/${id}`);
  }

  create(payload: CategoryPayload): Observable<Category> {
    return this.http.post<Category>(this.baseUrl, payload);
  }

  update(id: number, payload: CategoryPayload): Observable<Category> {
    return this.http.put<Category>(`${this.baseUrl}/${id}`, payload);
  }

  /** Rejected with 422 if the category still has products. */
  remove(id: number): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${id}`);
  }
}
