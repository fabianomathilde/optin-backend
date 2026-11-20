-- V2: merchant table (merchant list used to check R2)

CREATE TABLE merchant (
    merchant    VARCHAR(14) NOT NULL,
    created_at  DATE        NOT NULL,

    CONSTRAINT uq_merchant_merchant UNIQUE (merchant)
);
