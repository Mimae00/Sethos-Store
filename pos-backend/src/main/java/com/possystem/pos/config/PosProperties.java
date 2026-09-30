package com.possystem.pos.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.math.BigDecimal;

/**
 * Store level settings, bound from the {@code pos.*} keys in application.yml.
 *
 * <p>Exposed to the client through {@code GET /api/settings} so the terminal and receipts
 * pick up currency, tax and branding from configuration instead of hardcoding them.</p>
 */
@ConfigurationProperties(prefix = "pos")
public record PosProperties(
        String storeName,
        String storeAddress,
        String storePhone,
        String taxIdentifier,
        String currencyCode,
        String currencySymbol,
        /** IANA zone used for "today", daily reports and receipt numbering. */
        String timeZone,
        BigDecimal defaultTaxRate,
        String receiptFooter,
        /** Origins allowed to call the API. The Angular dev server sits here. */
        String[] allowedOrigins,
        /** Loads a demo catalogue on first start. Turn off for a real deployment. */
        Boolean seedDemoData
) {

    public PosProperties {
        seedDemoData = seedDemoData == null || seedDemoData;
        storeName = orDefault(storeName, "My Store");
        storeAddress = orDefault(storeAddress, "");
        storePhone = orDefault(storePhone, "");
        taxIdentifier = orDefault(taxIdentifier, "");
        currencyCode = orDefault(currencyCode, "USD");
        currencySymbol = orDefault(currencySymbol, "$");
        timeZone = orDefault(timeZone, "UTC");
        defaultTaxRate = defaultTaxRate == null ? BigDecimal.ZERO : defaultTaxRate;
        receiptFooter = orDefault(receiptFooter, "Thank you for your purchase!");
        allowedOrigins = allowedOrigins == null || allowedOrigins.length == 0
                ? new String[]{"http://localhost:4200"}
                : allowedOrigins;
    }

    private static String orDefault(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }
}
