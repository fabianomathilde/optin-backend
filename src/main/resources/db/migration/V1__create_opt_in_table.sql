-- V1: opt-in table (initial schema)
-- Migrations are append-only once applied: never edit this file after it has run anywhere; add V2__...sql instead.

CREATE SEQUENCE opt_in_id_seq AS INTEGER;

CREATE TABLE opt_in (
    id          INTEGER      NOT NULL DEFAULT nextval('opt_in_id_seq'),
    merchant    VARCHAR(14)  NOT NULL,
    grantee     VARCHAR(14)  NOT NULL,
    frequency   VARCHAR(10)  NOT NULL,
    start_date  DATE         NOT NULL,
    valid_until DATE,
    status      VARCHAR(10)  NOT NULL,
    revoked_at  TIMESTAMP WITH TIME ZONE,

    CONSTRAINT pk_opt_in PRIMARY KEY (id),
    CONSTRAINT ck_opt_in_frequency CHECK (frequency IN ('DAILY', 'WEEKLY', 'MONTHLY')),
    CONSTRAINT ck_opt_in_status CHECK (status IN ('ACTIVE', 'REVOKED')),
    CONSTRAINT ck_opt_in_dates CHECK (valid_until IS NULL OR valid_until >= start_date),
    CONSTRAINT ck_opt_in_revoked_at CHECK ((status = 'REVOKED') = (revoked_at IS NOT NULL))
);

ALTER SEQUENCE opt_in_id_seq OWNED BY opt_in.id;

-- At most one ACTIVE opt-in per (merchant, grantee). Revoked rows are not constrained.
CREATE UNIQUE INDEX uq_opt_in_active_pair ON opt_in (merchant, grantee) WHERE status = 'ACTIVE';
