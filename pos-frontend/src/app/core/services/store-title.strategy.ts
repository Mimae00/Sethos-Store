import { Injectable, inject } from '@angular/core';
import { Title } from '@angular/platform-browser';
import { RouterStateSnapshot, TitleStrategy } from '@angular/router';
import { SettingsStore } from './settings.store';

/**
 * Formats the browser tab as "Terminal · Sethos Store".
 *
 * Without this, each route's `title` replaces the document title outright and the store
 * name disappears from the tab. The store name comes from the API settings, so renaming
 * the store is still a config change.
 */
@Injectable({ providedIn: 'root' })
export class StoreTitleStrategy extends TitleStrategy {
  private readonly title = inject(Title);
  private readonly settings = inject(SettingsStore);

  override updateTitle(snapshot: RouterStateSnapshot): void {
    const page = this.buildTitle(snapshot);
    const store = this.settings.settings().storeName;
    this.title.setTitle(page ? `${page} · ${store}` : store);
  }
}
