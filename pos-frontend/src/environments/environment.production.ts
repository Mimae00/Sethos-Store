/**
 * Production settings.
 *
 * A same-origin relative path is the default: serve the built frontend behind the same
 * reverse proxy as the API and no CORS configuration is needed at all. Point this at an
 * absolute URL if the API lives on a different host.
 */
export const environment = {
  production: true,
  apiBaseUrl: '/api',
};
