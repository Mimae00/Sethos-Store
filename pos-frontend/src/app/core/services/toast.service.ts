import { Injectable, signal } from '@angular/core';
import { toFriendlyError } from '../api/api-error';

export type ToastTone = 'success' | 'error' | 'info';

export interface Toast {
  id: number;
  tone: ToastTone;
  message: string;
  details: string[];
}

/**
 * Lightweight notification queue. Errors auto-dismiss more slowly than successes because
 * they usually carry detail lines the cashier needs to read.
 */
@Injectable({ providedIn: 'root' })
export class ToastService {
  private nextId = 1;
  private readonly queue = signal<Toast[]>([]);

  readonly toasts = this.queue.asReadonly();

  success(message: string): void {
    this.push('success', message, [], 3000);
  }

  info(message: string): void {
    this.push('info', message, [], 3500);
  }

  error(message: string, details: string[] = []): void {
    this.push('error', message, details, 7000);
  }

  /** Converts a thrown HTTP failure into a readable toast. */
  fromError(error: unknown, fallback = 'Request failed'): void {
    const friendly = toFriendlyError(error);
    this.error(friendly.message || fallback, friendly.details);
  }

  dismiss(id: number): void {
    this.queue.update((toasts) => toasts.filter((toast) => toast.id !== id));
  }

  private push(tone: ToastTone, message: string, details: string[], ttlMs: number): void {
    const toast: Toast = { id: this.nextId++, tone, message, details };
    this.queue.update((toasts) => [...toasts, toast]);
    setTimeout(() => this.dismiss(toast.id), ttlMs);
  }
}
