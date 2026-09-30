import { HttpClient } from '@angular/common/http';
import { Injectable, inject, signal } from '@angular/core';
import { firstValueFrom } from 'rxjs';
import { API_BASE_URL } from '../api/api.tokens';
import { StoreSettings } from '../models/report.model';

/**
 * Store branding, currency and tax defaults, loaded once during app bootstrap.
 *
 * Kept in a signal store rather than fetched per component so the money pipe can format
 * synchronously and every screen agrees on the currency.
 */
@Injectable({ providedIn: 'root' })
export class SettingsStore {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = inject(API_BASE_URL);

  /** Used until the API responds, and as a fallback if it never does. */
  private static readonly FALLBACK: StoreSettings = {
    storeName: 'Sethos Store',
    storeAddress: '',
    storePhone: '',
    taxIdentifier: '',
    currencyCode: 'USD',
    currencySymbol: '$',
    timeZone: 'UTC',
    defaultTaxRate: 0,
    receiptFooter: '',
  };

  private readonly state = signal<StoreSettings>(SettingsStore.FALLBACK);
  private readonly loadFailed = signal(false);

  readonly settings = this.state.asReadonly();
  readonly offline = this.loadFailed.asReadonly();

  /**
   * Resolves even when the request fails: the app stays usable with fallback branding
   * rather than refusing to start because a cosmetic endpoint is down.
   */
  async load(): Promise<void> {
    try {
      const settings = await firstValueFrom(
        this.http.get<StoreSettings>(`${this.baseUrl}/settings`),
      );
      this.state.set(settings);
      this.loadFailed.set(false);
    } catch {
      this.loadFailed.set(true);
    }
  }
}
