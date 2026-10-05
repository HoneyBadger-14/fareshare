package com.fareshare.model;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public final class CountryCatalog {
    public record Country(String code, String name, String defaultCurrencyCode) {}

    public static final List<Country> COUNTRIES = List.of(
            new Country("AR", "Argentina", "ARS"),
            new Country("AU", "Australia", "AUD"),
            new Country("AT", "Austria", "EUR"),
            new Country("BE", "Belgium", "EUR"),
            new Country("CA", "Canada", "CAD"),
            new Country("DK", "Denmark", "DKK"),
            new Country("DE", "Germany", "EUR"),
            new Country("IN", "India", "INR"),
            new Country("IE", "Ireland", "EUR"),
            new Country("KE", "Kenya", "KES"),
            new Country("MY", "Malaysia", "MYR"),
            new Country("NL", "Netherlands", "EUR"),
            new Country("NZ", "New Zealand", "NZD"),
            new Country("NG", "Nigeria", "NGN"),
            new Country("NO", "Norway", "NOK"),
            new Country("PK", "Pakistan", "PKR"),
            new Country("PH", "Philippines", "PHP"),
            new Country("SG", "Singapore", "SGD"),
            new Country("ZA", "South Africa", "ZAR"),
            new Country("SE", "Sweden", "SEK"),
            new Country("GB", "United Kingdom", "GBP"),
            new Country("US", "United States", "USD")
    );

    public static final Set<String> CURRENCIES = COUNTRIES.stream()
            .map(Country::defaultCurrencyCode).collect(Collectors.toUnmodifiableSet());

    private CountryCatalog() {}
}
