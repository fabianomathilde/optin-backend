package com.hexagonal.optin.adapters.out.firestore;

import com.google.api.core.ApiFuture;
import com.hexagonal.optin.domain.DomainException;

import java.util.concurrent.ExecutionException;

/**
 * Blocks on Firestore futures. Domain exceptions thrown inside a transaction (e.g. R1) come back
 * wrapped; they are unwrapped so callers see the same exception as with the Postgres adapter.
 * Anything else is an infrastructure error.
 */
final class Futures {

    private Futures() {
    }

    static <T> T await(ApiFuture<T> future) {
        try {
            return future.get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new FirestoreAccessException("Interrupted while waiting for Firestore", e);
        } catch (ExecutionException e) {
            for (Throwable cause = e.getCause(); cause != null; cause = cause.getCause()) {
                if (cause instanceof DomainException domain) {
                    throw domain;
                }
            }
            throw new FirestoreAccessException("Firestore operation failed", e.getCause());
        }
    }

    /** Infrastructure failure talking to Firestore (not a business rule violation). */
    static final class FirestoreAccessException extends RuntimeException {
        FirestoreAccessException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
