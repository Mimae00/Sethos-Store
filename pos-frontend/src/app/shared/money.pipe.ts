import { Pipe, PipeTransform, inject } from '@angular/core';
import { SettingsStore } from '../core/services/settings.store';

/**
 * Formats an amount using the store's configured currency.
 *
 * Reads the settings signal directly instead of taking the currency as an argument, so
 * every template stays `{{ amount | money }}` and a currency change is a config edit.
 * The app shell waits for settings before rendering, so the code is never stale here.
 */
@Pipe({ name: 'money' })
export class MoneyPipe implements PipeTransform {
  private readonly settings = inject(SettingsStore);

  transform(value: number | null | undefined): string {
    const amount = typeof value === 'number' && Number.isFinite(value) ? value : 0;
    const { currencyCode, currencySymbol } = this.settings.settings();

    try {
      return new Intl.NumberFormat(undefined, {
        style: 'currency',
        currency: currencyCode,
        minimumFractionDigits: 2,
        maximumFractionDigits: 2,
      }).format(amount);
    } catch {
      // An unknown ISO code would make Intl throw; fall back to the configured symbol.
      return `${currencySymbol}${amount.toFixed(2)}`;
    }
  }
}
