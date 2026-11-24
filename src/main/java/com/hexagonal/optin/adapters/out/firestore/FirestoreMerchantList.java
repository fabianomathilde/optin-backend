package com.hexagonal.optin.adapters.out.firestore;

import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.Firestore;
import com.hexagonal.optin.application.port.out.MerchantList;
import com.hexagonal.optin.domain.Merchant;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.Map;

import static com.hexagonal.optin.adapters.out.firestore.Futures.await;

/**
 * Driven adapter for {@link MerchantList} backed by Firestore: {@code merchants/{cnpj}}.
 * A document present in the collection means the merchant is on the list (R2).
 */
@Component
@ConditionalOnProperty(name = "optin.persistence", havingValue = "firestore")
class FirestoreMerchantList implements MerchantList {

    static final String MERCHANTS = "merchants";

    private final Firestore db;

    FirestoreMerchantList(Firestore db) {
        this.db = db;
    }

    @Override
    public boolean isActive(String merchant) {
        return await(db.collection(MERCHANTS).document(merchant).get()).exists();
    }

    /** Idempotent: inside a transaction, the document is written only if it doesn't exist yet. */
    @Override
    public void add(Merchant merchant) {
        DocumentReference document = db.collection(MERCHANTS).document(merchant.document());
        await(db.runTransaction(tx -> {
            if (!tx.get(document).get().exists()) {
                tx.set(document, Map.of("registeredOn", merchant.registeredOn().toString()));
            }
            return null;
        }));
    }
}
