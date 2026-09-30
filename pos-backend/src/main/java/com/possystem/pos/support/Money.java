package com.possystem.pos.support;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Every currency amount in the system passes through here.
 *
 * <p>Two decimals, HALF_UP. Centralised on purpose: inconsistent scale or rounding is the
 * usual source of receipts that are off by a cent, and those are painful to chase down
 * after the fact.</p>
 */
public final class Money {

    public static final int SCALE = 2;
    public static final RoundingMode ROUNDING = RoundingMode.HALF_UP;
    public static final BigDecimal ZERO = BigDecimal.ZERO.setScale(SCALE);

    private Money() {
    }

    /** Null-safe normalisation to the canonical scale. */
    public static BigDecimal of(BigDecimal value) {
        return value == null ? ZERO : value.setScale(SCALE, ROUNDING);
    }

    public static BigDecimal of(long value) {
        return BigDecimal.valueOf(value).setScale(SCALE, ROUNDING);
    }

    public static BigDecimal add(BigDecimal a, BigDecimal b) {
        return of(of(a).add(of(b)));
    }

    public static BigDecimal subtract(BigDecimal a, BigDecimal b) {
        return of(of(a).subtract(of(b)));
    }

    public static BigDecimal multiply(BigDecimal amount, int multiplier) {
        return of(of(amount).multiply(BigDecimal.valueOf(multiplier)));
    }

    /** Applies a percentage such as 12.00 to an amount. */
    public static BigDecimal percentageOf(BigDecimal amount, BigDecimal percentage) {
        if (percentage == null || percentage.signum() == 0) {
            return ZERO;
        }
        return of(of(amount)
                .multiply(percentage)
                .divide(BigDecimal.valueOf(100), SCALE + 4, ROUNDING));
    }

    public static BigDecimal divide(BigDecimal amount, long divisor) {
        if (divisor == 0) {
            return ZERO;
        }
        return of(of(amount).divide(BigDecimal.valueOf(divisor), SCALE, ROUNDING));
    }

    /** Clamps negatives to zero; a receipt should never show a negative amount due. */
    public static BigDecimal atLeastZero(BigDecimal value) {
        BigDecimal normalised = of(value);
        return normalised.signum() < 0 ? ZERO : normalised;
    }

    public static BigDecimal min(BigDecimal a, BigDecimal b) {
        return of(a).compareTo(of(b)) <= 0 ? of(a) : of(b);
    }

    public static boolean isNegative(BigDecimal value) {
        return value != null && value.signum() < 0;
    }
}
