package com.fareshare.service;

import com.fareshare.model.CountryCatalog;
import com.fareshare.controller.ApiError;
import java.util.Currency;
import java.util.Locale;

final class CurrencyRules {
    private CurrencyRules() {}

    static String requireSupported(String code) {
        if (code == null || !CountryCatalog.CURRENCIES.contains(code.trim().toUpperCase(Locale.ROOT))) {
            throw ApiError.bad("Unsupported currency");
        }
        String normalized = code.trim().toUpperCase(Locale.ROOT);
        if (Currency.getInstance(normalized).getDefaultFractionDigits() < 0) {
            throw ApiError.bad("Unsupported currency precision");
        }
        return normalized;
    }
}
