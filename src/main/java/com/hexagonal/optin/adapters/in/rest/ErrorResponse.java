package com.hexagonal.optin.adapters.in.rest;

/** HTTP error body (see optin-api.yaml, schema Error). */
public record ErrorResponse(String message) {
}
