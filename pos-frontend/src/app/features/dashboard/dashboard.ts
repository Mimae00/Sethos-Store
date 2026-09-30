import { DatePipe, DecimalPipe } from '@angular/common';
import { Component, computed, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { PAYMENT_METHODS } from '../../core/models/sale.model';
import { Dashboard as DashboardData } from '../../core/models/report.model';
import { ReportService } from '../../core/services/report.service';
import { ToastService } from '../../core/services/toast.service';
import { MoneyPipe } from '../../shared/money.pipe';

/**
 * Landing screen. One request to {@code /api/reports/dashboard} fills the whole page,
 * which keeps it fast and means the figures are all from the same instant.
 */
@Component({
  selector: 'app-dashboard',
  imports: [RouterLink, DatePipe, DecimalPipe, MoneyPipe],
  templateUrl: './dashboard.html',
  styleUrl: './dashboard.css',
})
export class Dashboard {
  private readonly reports = inject(ReportService);
  private readonly toasts = inject(ToastService);

  protected readonly data = signal<DashboardData | null>(null);
  protected readonly loading = signal(true);
  protected readonly failed = signal(false);

  constructor() {
    this.load();
  }

  /** Tallest bar in the chart, used to scale the rest. Never zero, to avoid dividing by it. */
  protected readonly chartPeak = computed(() => {
    const points = this.data()?.dailySales ?? [];
    return Math.max(1, ...points.map((point) => point.total));
  });

  protected readonly hasSalesInChart = computed(() =>
    (this.data()?.dailySales ?? []).some((point) => point.saleCount > 0),
  );

  protected load(): void {
    this.loading.set(true);
    this.failed.set(false);
    this.reports.dashboard().subscribe({
      next: (data) => {
        this.data.set(data);
        this.loading.set(false);
      },
      error: (error: unknown) => {
        this.loading.set(false);
        this.failed.set(true);
        this.toasts.fromError(error, 'Could not load the dashboard');
      },
    });
  }

  protected barHeight(total: number): string {
    // Floor at 2% so a day with a tiny sale is still visible.
    return `${Math.max(2, (total / this.chartPeak()) * 100)}%`;
  }

  protected paymentLabel(method: string): string {
    return PAYMENT_METHODS.find((entry) => entry.value === method)?.label ?? method;
  }

  /** Share of takings for a tender type, as a percentage of the day's total. */
  protected paymentShare(total: number): number {
    const collected = this.data()?.today.totalCollected ?? 0;
    return collected > 0 ? (total / collected) * 100 : 0;
  }

  protected marginPercent(): number {
    const summary = this.data()?.today;
    if (!summary || summary.netSales <= 0) {
      return 0;
    }
    return (summary.grossProfit / summary.netSales) * 100;
  }
}
