/** Mirrors the backend's PageResponse<T> envelope. */
export interface Page<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  first: boolean;
  last: boolean;
}

/** Mirrors the backend's ApiError, returned by every failing request. */
export interface ApiError {
  timestamp: string;
  status: number;
  error: string;
  message: string;
  path: string;
  /** Field name to message. Present on 400 validation failures. */
  fieldErrors?: Record<string, string>;
  /** Extra lines, e.g. one per product that ran out of stock. */
  details?: string[];
}

export function emptyPage<T>(size = 20): Page<T> {
  return {
    content: [],
    page: 0,
    size,
    totalElements: 0,
    totalPages: 0,
    first: true,
    last: true,
  };
}
