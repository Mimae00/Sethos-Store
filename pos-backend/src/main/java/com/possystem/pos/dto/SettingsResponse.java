package com.possystem.pos.dto;

import com.possystem.pos.config.PosProperties;

import java.math.BigDecimal;

/**
 * Store branding, currency and tax defaults handed to the client at startup.
 */
public record SettingsResponse(
        String storeName,
        String storeAddress,
        String storePhone,
        String taxIdentifier,
        String currencyCode,
        String currencySymbol,
        String timeZone,
        BigDecimal defaultTaxRate,
        String receiptFooter
) {

    public static SettingsResponse from(PosProperties properties) {
        return new SettingsResponse(
                properties.storeName(),
                properties.storeAddress(),
                properties.storePhone(),
                properties.taxIdentifier(),
                properties.currencyCode(),
                properties.currencySymbol(),
                properties.timeZone(),
                properties.defaultTaxRate(),
                properties.receiptFooter()
        );
    }
}
