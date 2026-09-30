import { provideHttpClient, withFetch } from '@angular/common/http';
import {
  ApplicationConfig,
  inject,
  provideAppInitializer,
  provideBrowserGlobalErrorListeners,
} from '@angular/core';
import {
  TitleStrategy,
  provideRouter,
  withComponentInputBinding,
  withInMemoryScrolling,
} from '@angular/router';
import { environment } from '../environments/environment';
import { API_BASE_URL } from './core/api/api.tokens';
import { SettingsStore } from './core/services/settings.store';
import { StoreTitleStrategy } from './core/services/store-title.strategy';
import { routes } from './app.routes';

export const appConfig: ApplicationConfig = {
  providers: [
    provideBrowserGlobalErrorListeners(),
    provideRouter(
      routes,
      // Lets route params bind straight to component inputs.
      withComponentInputBinding(),
      withInMemoryScrolling({ scrollPositionRestoration: 'top' }),
    ),
    { provide: TitleStrategy, useClass: StoreTitleStrategy },
    provideHttpClient(withFetch()),
    { provide: API_BASE_URL, useValue: environment.apiBaseUrl },
    // Currency and branding are needed before the first amount is rendered, so settings
    // are fetched during bootstrap. The store resolves even on failure.
    provideAppInitializer(() => inject(SettingsStore).load()),
  ],
};
