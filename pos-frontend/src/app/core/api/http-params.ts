import { HttpParams } from '@angular/common/http';

/**
 * Builds query parameters, dropping anything null, undefined or an empty string.
 *
 * Without this, an untouched filter would be sent as `search=` and the backend would
 * treat it as a real (empty) filter rather than "no filter".
 */
export function toHttpParams(source: Record<string, unknown>): HttpParams {
  let params = new HttpParams();
  for (const [key, value] of Object.entries(source)) {
    if (value === null || value === undefined || value === '') {
      continue;
    }
    params = params.set(key, String(value));
  }
  return params;
}
