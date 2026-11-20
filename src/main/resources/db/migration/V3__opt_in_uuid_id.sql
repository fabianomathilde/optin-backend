-- V3: opt-in ids become UUIDs, assigned by the application (ADR 0005).
-- Append-only: V1 is never edited. Existing rows (dev data only) receive new random ids.

ALTER TABLE opt_in ALTER COLUMN id DROP DEFAULT;
ALTER TABLE opt_in DROP CONSTRAINT pk_opt_in;
ALTER TABLE opt_in ALTER COLUMN id TYPE uuid USING gen_random_uuid();
ALTER TABLE opt_in ADD CONSTRAINT pk_opt_in PRIMARY KEY (id);

DROP SEQUENCE opt_in_id_seq;
