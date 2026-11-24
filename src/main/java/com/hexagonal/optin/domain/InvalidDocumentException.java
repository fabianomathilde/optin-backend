package com.hexagonal.optin.domain;

/** The document is not in CNPJ format (14 digits, no punctuation). */
public class InvalidDocumentException extends DomainException {

    public InvalidDocumentException(String document) {
        super("Document " + document + " is not a valid CNPJ (14 digits)");
    }
}
