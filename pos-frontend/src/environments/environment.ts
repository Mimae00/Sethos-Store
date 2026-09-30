/**
 * Development settings.
 *
 * The API runs on its own origin, so this is an absolute URL. The backend allows
 * http://localhost:4200 via `pos.allowed-origins` in application.yml.
 */
export const environment = {
  production: false,
  apiBaseUrl: 'http://localhost:8080/api',
};
