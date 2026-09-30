import { Component, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Page, emptyPage } from '../../core/models/common.model';
import { PAYMENT_METHODS, Sale, SaleStatus, SaleSummary } from '../../core/models/sale.model';
import { SaleService } from '../../core/services/sale.service';
import { ToastService } from '../../core/services/toast.service';
import { MoneyPipe } from '../../shared/money.pipe';
import { Receipt } from '../../shared/receipt';
import { DatePipe } from '@angular/common';

/**
 * Sales history: filter, open a receipt, reprint, and void.
 *
 * <p>Sales are never deleted. Voiding keeps the row, records who and why, and returns the
 * stock, which is what an audit of the till actually needs.</p>
 */
@Component({
  selector: 'app-sales',
  imports: [FormsModule, DatePipe, MoneyPipe, Receipt],
  templateUrl: './sales.html',
  styleUrl: './sales.css',
})
export class Sales {
  private readonly service = inject(SaleService);
  private readonly toasts = inject(ToastService);

  protected readonly page = signal<Page<SaleSummary>>(emptyPage<SaleSummary>());
  protected readonly loading = signal(false);
  protected readonly working = signal(false);

  protected readonly search = signal('');
  protected readonly status = signal<SaleStatus | ''>('');
  protected readonly from = signal('');
  protected readonly to = signal('');
  protected readonly pageIndex = signal(0);

  protected readonly selected = signal<Sale | null>(null);
  protected readonly voidTarget = signal<Sale | null>(null);
  protected readonly voidReason = signal('');

  protected readonly statuses: readonly { value: SaleStatus | ''; label: string }[] = [
    { value: '', label: 'All statuses' },
    { value: 'COMPLETED', label: 'Completed' },
    { value: 'VOIDED', label: 'Voided' },
    { value: 'REFUNDED', label: 'Refunded' },
  ];

  constructor() {
    this.load();
  }

  protected readonly rangeLabel = computed(() => {
    const current = this.page();
    if (current.totalElements === 0) {
      return 'No sales';
    }
    const first = current.page * current.size + 1;
    const last = Math.min(first + current.content.length - 1, current.totalElements);
    return `${first}–${last} of ${current.totalElements}`;
  });

  /** Total of the rows on screen, so a filtered day can be read off directly. */
  protected readonly pageTotal = computed(() =>
    this.page()
      .content.filter((sale) => sale.status === 'COMPLETED')
      .reduce((sum, sale) => sum + sale.total, 0),
  );

  protected load(): void {
    this.loading.set(true);
    this.service
      .search({
        search: this.search().trim(),
        status: this.status() || null,
        from: this.from() || null,
        to: this.to() || null,
        page: this.pageIndex(),
        size: 20,
      })
      .subscribe({
        next: (page) => {
          this.page.set(page);
          this.loading.set(false);
        },
        error: (error: unknown) => {
          this.loading.set(false);
          this.toasts.fromError(error, 'Could not load sales');
        },
      });
  }

  protected applyFilters(): void {
    this.pageIndex.set(0);
    this.load();
  }

  protected clearFilters(): void {
    this.search.set('');
    this.status.set('');
    this.from.set('');
    this.to.set('');
    this.applyFilters();
  }

  protected goToPage(index: number): void {
    this.pageIndex.set(Math.max(0, Math.min(index, this.page().totalPages - 1)));
    this.load();
  }

  /** The list endpoint omits line items, so the receipt is fetched on demand. */
  protected openSale(summary: SaleSummary): void {
    this.working.set(true);
    this.service.get(summary.id).subscribe({
      next: (sale) => {
        this.working.set(false);
        this.selected.set(sale);
      },
      error: (error: unknown) => {
        this.working.set(false);
        this.toasts.fromError(error, 'Could not load the receipt');
      },
    });
  }

  protected closeSale(): void {
    this.selected.set(null);
    document.body.classList.remove('printing-receipt');
  }

  protected printReceipt(): void {
    document.body.classList.add('printing-receipt');
    window.print();
    document.body.classList.remove('printing-receipt');
  }

  protected askVoid(sale: Sale): void {
    this.voidReason.set('');
    this.voidTarget.set(sale);
  }

  protected cancelVoid(): void {
    this.voidTarget.set(null);
  }

  protected confirmVoid(): void {
    const sale = this.voidTarget();
    const reason = this.voidReason().trim();
    if (!sale || !reason) {
      return;
    }
    this.working.set(true);
    this.service.void(sale.id, reason).subscribe({
      next: (voided) => {
        this.working.set(false);
        this.voidTarget.set(null);
        this.selected.set(voided);
        this.toasts.success(`${voided.reference} voided and stock returned`);
        this.load();
      },
      error: (error: unknown) => {
        this.working.set(false);
        this.toasts.fromError(error, 'Could not void the sale');
      },
    });
  }

  protected statusBadge(status: SaleStatus): string {
    switch (status) {
      case 'COMPLETED':
        return 'badge--success';
      case 'VOIDED':
        return 'badge--danger';
      default:
        return 'badge--warning';
    }
  }

  protected paymentLabel(method: string): string {
    return PAYMENT_METHODS.find((entry) => entry.value === method)?.label ?? method;
  }
}
