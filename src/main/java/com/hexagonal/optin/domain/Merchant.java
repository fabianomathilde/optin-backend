package com.hexagonal.optin.domain;

import java.time.LocalDate;
import java.util.Objects;

/**
 * A merchant on the merchant list (R2). Being on the list is what makes a merchant active.
 * The document is a CNPJ: 14 digits, no punctuation (format only; check digits are a later step).
 */
public record Merchant(String document, LocalDate registeredOn) {

    private static final String CNPJ_FORMAT = "\\d{14}";

    public Merchant {
        Objects.requireNonNull(registeredOn, "registeredOn");
        if (document == null || !document.matches(CNPJ_FORMAT)) {
            throw new InvalidDocumentException(document);
        }
    }
}
