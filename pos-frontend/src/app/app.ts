import { Component, computed, inject } from '@angular/core';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { CartStore } from './core/services/cart.store';
import { SettingsStore } from './core/services/settings.store';
import { ToastHost } from './shared/toast-host';

interface NavItem {
  path: string;
  label: string;
  hint: string;
}

@Component({
  selector: 'app-root',
  imports: [RouterOutlet, RouterLink, RouterLinkActive, ToastHost],
  templateUrl: './app.html',
  styleUrl: './app.css',
})
export class App {
  private readonly settingsStore = inject(SettingsStore);
  protected readonly cart = inject(CartStore);

  protected readonly settings = this.settingsStore.settings;
  protected readonly apiOffline = this.settingsStore.offline;

  protected readonly navItems: readonly NavItem[] = [
    { path: '/terminal', label: 'Terminal', hint: 'Ring up a sale' },
    { path: '/dashboard', label: 'Dashboard', hint: 'Today at a glance' },
    { path: '/products', label: 'Products', hint: 'Catalogue and stock' },
    { path: '/categories', label: 'Categories', hint: 'Group the catalogue' },
    { path: '/customers', label: 'Customers', hint: 'Buyer records' },
    { path: '/sales', label: 'Sales', hint: 'History and receipts' },
  ];

  /** Shown as a badge on the Terminal link so an abandoned basket is not forgotten. */
  protected readonly basketCount = computed(() => this.cart.unitCount());

  /** Logo mark: initials of the store name, e.g. "Sethos Store" becomes "SS". */
  protected readonly brandInitials = computed(() =>
    this.settings()
      .storeName.split(/\s+/)
      .filter(Boolean)
      .slice(0, 2)
      .map((word) => word[0]!.toUpperCase())
      .join(''),
  );
}
