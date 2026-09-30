import { InjectionToken } from '@angular/core';

/**
 * Base URL for the REST API, e.g. {@code http://localhost:8080/api}.
 *
 * Injected rather than imported from the environment directly so tests and alternative
 * deployments can override it without touching service code.
 */
export const API_BASE_URL = new InjectionToken<string>('API_BASE_URL');
