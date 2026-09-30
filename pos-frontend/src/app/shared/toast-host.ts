import { Component, inject } from '@angular/core';
import { ToastService } from '../core/services/toast.service';

@Component({
  selector: 'app-toast-host',
  template: `
    <div class="toast-host" role="status" aria-live="polite">
      @for (toast of toasts.toasts(); track toast.id) {
        <div class="toast toast--{{ toast.tone }}">
          <div class="toast__body">
            <p class="toast__message">{{ toast.message }}</p>
            @if (toast.details.length) {
              <ul class="toast__details">
                @for (detail of toast.details; track detail) {
                  <li>{{ detail }}</li>
                }
              </ul>
            }
          </div>
          <button
            type="button"
            class="toast__close"
            aria-label="Dismiss notification"
            (click)="toasts.dismiss(toast.id)"
          >
            &times;
          </button>
        </div>
      }
    </div>
  `,
  styles: `
    .toast-host {
      position: fixed;
      top: 1rem;
      right: 1rem;
      z-index: 1000;
      display: flex;
      flex-direction: column;
      gap: 0.5rem;
      width: min(24rem, calc(100vw - 2rem));
      pointer-events: none;
    }

    .toast {
      pointer-events: auto;
      display: flex;
      align-items: flex-start;
      gap: 0.5rem;
      padding: 0.75rem 0.875rem;
      border-radius: 0.625rem;
      border-left: 4px solid var(--tone);
      background: var(--surface);
      box-shadow: 0 10px 30px rgb(15 23 42 / 18%);
    }

    .toast--success { --tone: #16a34a; }
    .toast--error { --tone: #dc2626; }
    .toast--info { --tone: #2563eb; }

    .toast__body { flex: 1; min-width: 0; }

    .toast__message {
      margin: 0;
      font-weight: 600;
      font-size: 0.875rem;
      color: var(--text);
    }

    .toast__details {
      margin: 0.375rem 0 0;
      padding-left: 1.1rem;
      font-size: 0.8125rem;
      color: var(--text-muted);
    }

    .toast__close {
      border: 0;
      background: none;
      cursor: pointer;
      font-size: 1.25rem;
      line-height: 1;
      color: var(--text-muted);
      padding: 0 0.25rem;
    }

    .toast__close:hover { color: var(--text); }
  `,
})
export class ToastHost {
  protected readonly toasts = inject(ToastService);
}
