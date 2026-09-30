import { HttpErrorResponse } from '@angular/common/http';
import { ApiError } from '../models/common.model';

/** A failure translated into something worth showing a cashier. */
export interface FriendlyError {
  message: string;
  /** Extra lines, e.g. one per product that ran out of stock. */
  details: string[];
  /** Field name to message, for inline form errors. */
  fieldErrors: Record<string, string>;
  status: number;
}

function isApiError(body: unknown): body is ApiError {
  return (
    typeof body === 'object' &&
    body !== null &&
    'status' in body &&
    'message' in body
  );
}

/**
 * Normalises anything the HTTP layer can throw into one shape.
 *
 * The backend returns a consistent ApiError body, but network failures and non-JSON
 * responses do not, and those are exactly the cases where a raw error object leaking into
 * the UI is least helpful.
 */
export function toFriendlyError(error: unknown): FriendlyError {
  if (error instanceof HttpErrorResponse) {
    // Status 0 means the request never reached the server.
    if (error.status === 0) {
      return {
        message: 'Cannot reach the server. Check that the API is running on port 8080.',
        details: [],
        fieldErrors: {},
        status: 0,
      };
    }

    const body: unknown = error.error;
    if (isApiError(body)) {
      const fieldErrors = body.fieldErrors ?? {};
      return {
        message: body.message || error.statusText || 'Request failed',
        // Validation messages live in fieldErrors; surface them as detail lines too so a
        // toast is still informative when there is no form to attach them to.
        details: body.details?.length ? body.details : Object.values(fieldErrors),
        fieldErrors,
        status: body.status,
      };
    }

    return {
      message: typeof body === 'string' && body ? body : error.message,
      details: [],
      fieldErrors: {},
      status: error.status,
    };
  }

  if (error instanceof Error) {
    return { message: error.message, details: [], fieldErrors: {}, status: 0 };
  }

  return { message: 'Something went wrong', details: [], fieldErrors: {}, status: 0 };
}
