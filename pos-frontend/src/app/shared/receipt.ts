import { DatePipe } from '@angular/common';
import { Component, computed, inject, input } from '@angular/core';
import { SettingsStore } from '../core/services/settings.store';
import { Sale } from '../core/models/sale.model';
import { PAYMENT_METHODS } from '../core/models/sale.model';
import { MoneyPipe } from './money.pipe';

/**
 * Printable receipt. Shared by the terminal (straight after checkout) and the sales
 * history, so a reprint is guaranteed to look like the original.
 */
@Component({
  selector: 'app-receipt',
  imports: [DatePipe, MoneyPipe],
  template: `
    <article class="receipt">
      <header class="receipt__head">
        <h3 class="receipt__store">{{ settings().storeName }}</h3>
        @if (settings().storeAddress) {
          <p class="receipt__line">{{ settings().storeAddress }}</p>
        }
        @if (settings().storePhone) {
          <p class="receipt__line">{{ settings().storePhone }}</p>
        }
        @if (settings().taxIdentifier) {
          <p class="receipt__line">{{ settings().taxIdentifier }}</p>
        }
      </header>

      <dl class="receipt__meta">
        <div><dt>Receipt</dt><dd>{{ sale().reference }}</dd></div>
        <div><dt>Date</dt><dd>{{ sale().soldAt | date: 'medium' }}</dd></div>
        <div><dt>Cashier</dt><dd>{{ sale().cashierName }}</dd></div>
        <div><dt>Payment</dt><dd>{{ paymentLabel() }}</dd></div>
        @if (sale().customer) {
          <div><dt>Customer</dt><dd>{{ sale().customer!.name }}</dd></div>
        }
      </dl>

      @if (sale().status !== 'COMPLETED') {
        <p class="receipt__void">
          {{ sale().status }}@if (sale().voidReason) { &mdash; {{ sale().voidReason }} }
        </p>
      }

      <table class="receipt__items">
        <thead>
          <tr>
            <th scope="col">Item</th>
            <th scope="col" class="numeric">Qty</th>
            <th scope="col" class="numeric">Price</th>
            <th scope="col" class="numeric">Total</th>
          </tr>
        </thead>
        <tbody>
          @for (item of sale().items; track item.id) {
            <tr>
              <td>
                <span class="receipt__item-name">{{ item.productName }}</span>
                <span class="receipt__item-sku">{{ item.productSku }}</span>
                @if (item.discountAmount > 0) {
                  <span class="receipt__item-note">
                    less {{ item.discountAmount | money }} discount
                  </span>
                }
              </td>
              <td class="numeric">{{ item.quantity }} {{ item.unit }}</td>
              <td class="numeric">{{ item.unitPrice | money }}</td>
              <td class="numeric">{{ item.lineTotal | money }}</td>
            </tr>
          }
        </tbody>
      </table>

      <dl class="receipt__totals">
        <div><dt>Subtotal</dt><dd>{{ sale().subtotal | money }}</dd></div>
        @if (sale().discountTotal > 0) {
          <div><dt>Discounts</dt><dd>-{{ sale().discountTotal | money }}</dd></div>
        }
        @if (sale().taxTotal > 0) {
          <div><dt>Tax</dt><dd>{{ sale().taxTotal | money }}</dd></div>
        }
        <div class="receipt__grand"><dt>Total</dt><dd>{{ sale().total | money }}</dd></div>
        <div><dt>Tendered</dt><dd>{{ sale().amountTendered | money }}</dd></div>
        <div><dt>Change</dt><dd>{{ sale().changeDue | money }}</dd></div>
      </dl>

      @if (sale().note) {
        <p class="receipt__note">{{ sale().note }}</p>
      }

      @if (settings().receiptFooter) {
        <p class="receipt__footer">{{ settings().receiptFooter }}</p>
      }
    </article>
  `,
  styles: `
    .receipt {
      max-width: 22rem;
      margin: 0 auto;
      padding: 1rem;
      background: #fff;
      color: #0f172a;
      font-size: 0.8125rem;
      line-height: 1.45;
    }

    .receipt__head {
      text-align: center;
      padding-bottom: 0.75rem;
      border-bottom: 1px dashed var(--border-strong);
    }

    .receipt__store { font-size: 1rem; }

    .receipt__line {
      font-size: 0.75rem;
      color: var(--text-muted);
    }

    .receipt__meta {
      margin: 0.75rem 0;
      display: grid;
      gap: 0.125rem;
      font-size: 0.75rem;
    }

    .receipt__meta > div,
    .receipt__totals > div {
      display: flex;
      justify-content: space-between;
      gap: 0.75rem;
    }

    .receipt__meta dt,
    .receipt__totals dt {
      color: var(--text-muted);
      margin: 0;
    }

    .receipt__meta dd,
    .receipt__totals dd {
      margin: 0;
      font-variant-numeric: tabular-nums;
      text-align: right;
    }

    .receipt__void {
      margin: 0.5rem 0;
      padding: 0.375rem 0.5rem;
      border-radius: var(--radius-sm);
      background: var(--danger-soft);
      color: var(--danger);
      font-weight: 700;
      text-align: center;
      font-size: 0.75rem;
    }

    .receipt__items {
      width: 100%;
      border-collapse: collapse;
      border-top: 1px dashed var(--border-strong);
      border-bottom: 1px dashed var(--border-strong);
    }

    .receipt__items th,
    .receipt__items td {
      padding: 0.3125rem 0.25rem;
      vertical-align: top;
      font-size: 0.75rem;
    }

    .receipt__items th {
      text-transform: uppercase;
      letter-spacing: 0.03em;
      font-size: 0.6875rem;
      color: var(--text-muted);
      text-align: left;
    }

    .receipt__items .numeric { text-align: right; }

    .receipt__item-name { display: block; font-weight: 600; }

    .receipt__item-sku,
    .receipt__item-note {
      display: block;
      font-size: 0.6875rem;
      color: var(--text-muted);
    }

    .receipt__totals {
      margin: 0.75rem 0 0;
      display: grid;
      gap: 0.1875rem;
      font-size: 0.8125rem;
    }

    .receipt__grand {
      margin-top: 0.25rem;
      padding-top: 0.375rem;
      border-top: 1px solid var(--border-strong);
      font-weight: 700;
      font-size: 0.9375rem;
    }

    .receipt__note {
      margin-top: 0.75rem;
      font-size: 0.75rem;
      font-style: italic;
      color: var(--text-muted);
    }

    .receipt__footer {
      margin-top: 0.875rem;
      padding-top: 0.625rem;
      border-top: 1px dashed var(--border-strong);
      text-align: center;
      font-size: 0.75rem;
      color: var(--text-muted);
    }
  `,
})
export class Receipt {
  readonly sale = input.required<Sale>();

  protected readonly settings = inject(SettingsStore).settings;

  protected readonly paymentLabel = computed(
    () =>
      PAYMENT_METHODS.find((method) => method.value === this.sale().paymentMethod)?.label ??
      this.sale().paymentMethod,
  );
}
