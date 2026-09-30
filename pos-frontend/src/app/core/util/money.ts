/**
 * Two-decimal money helpers that mirror the backend's {@code Money} class.
 *
 * The cart previews totals locally so the cashier sees them update instantly, but the
 * server recomputes everything on checkout and its answer is authoritative. These helpers
 * exist so the preview and the receipt agree: same order of operations, same rounding.
 */

/**
 * Rounds half-up to 2 decimals.
 *
 * The epsilon nudge compensates for binary floating point, where a value such as
 * 1.005 is actually stored slightly below and would otherwise round down.
 */
export function round2(value: number): number {
  if (!Number.isFinite(value)) {
    return 0;
  }
  const scaled = value * 100;
  const corrected = scaled >= 0 ? scaled + Number.EPSILON * Math.abs(scaled) : scaled - Number.EPSILON * Math.abs(scaled);
  return Math.round(corrected) / 100;
}

export function clampToZero(value: number): number {
  return value < 0 ? 0 : value;
}

/** Applies a percentage such as 10 (meaning 10%) to an amount. */
export function percentageOf(amount: number, percentage: number): number {
  if (!percentage) {
    return 0;
  }
  return round2((amount * percentage) / 100);
}

/** Parses free-typed numeric input, tolerating empty strings and stray spaces. */
export function parseAmount(value: string | number | null | undefined): number {
  if (typeof value === 'number') {
    return Number.isFinite(value) ? value : 0;
  }
  if (value === null || value === undefined) {
    return 0;
  }
  const parsed = Number(String(value).replace(/,/g, '').trim());
  return Number.isFinite(parsed) ? parsed : 0;
}
