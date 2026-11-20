# 0005 — Firestore as the cloud persistence adapter

- **Status:** Accepted (2026-11-20)
- **Date:** 2026-11-20
- **Phase:** cross-cutting (persistence; supersedes the Cloud SQL part of ADR 0004)

## Context

The GCP environment is a learning environment paid from credit. Cloud SQL (`db-f1-micro`) plus its
proxy sidecar and private networking cost ~$10/month even when idle, and Cloud SQL is the slowest
piece to create. Firestore has a free tier: 1 GiB stored, 50,000 reads, 20,000 writes and 20,000
deletes per day, for exactly one database per project.

The core already isolates storage behind driven ports (`OptInRepository`, `MerchantList`), and
`OptInRepositoryContractTest` defines the behaviour every implementation must have.

## Decision

1. **Add a Firestore driven adapter; keep the PostgreSQL adapter.** Domain rules and use cases do
   not change.
   - `postgres` (default): local development with Podman, existing tests.
   - `firestore` (profile): GCP environments; local runs against the emulator.
   Selected by `optin.persistence=postgres|firestore` (adapters use `@ConditionalOnProperty`).
   The `firestore` profile excludes `DataSourceAutoConfiguration` (JPA, Spring Data JPA and the
   JDBC health check back off without a DataSource) and disables Flyway.
2. **Package:** `adapters/out/firestore`, including the client configuration, so no Firestore type
   leaves the adapter. Client: `com.google.cloud:google-cloud-firestore` (via `libraries-bom`).
   Credentials: Application Default Credentials (Workload Identity in GKE).
3. **Opt-in ids become UUIDs** (decided in review). The port takes `findById(UUID)`, the REST
   contract exposes `id` as `string/uuid` (OpenAPI 0.2.0), PostgreSQL migrates the column with
   Flyway `V3__opt_in_uuid_id.sql`. Repositories assign the UUID when saving a new opt-in, so no
   counter document or sequence is needed in either store.
4. **Data model (Firestore Native, database `(default)`):**

   | Collection / document | Fields | Purpose |
   |---|---|---|
   | `optIns/{uuid}` | `merchant`, `grantee`, `frequency`, `startDate`, `validUntil` (ISO `yyyy-MM-dd` strings, so they sort correctly), `status`, `revokedAt` (timestamp or null) | the aggregate |
   | `activeOptIns/{merchant}_{grantee}` | `optInId` | **R1 guard**: exists only while the pair has an ACTIVE opt-in |
   | `merchants/{cnpj}` | `registeredOn` | `MerchantList` (R2) |

5. **R1 (one ACTIVE opt-in per pair) is enforced in one transaction per save**, replacing the
   partial unique index: read the guard; if the opt-in is ACTIVE and the guard belongs to another
   opt-in → `DuplicateActiveOptInException` (same exception as the Postgres adapter); otherwise
   write the opt-in and set the guard (ACTIVE) or delete it (REVOKED). Firestore transactions
   serialize concurrent grants for the same pair; a test runs four concurrent grants and expects
   exactly one to commit.
6. **Queries and indexes:** `findByGrantee` / `findByMerchant` use equality filters;
   `findActiveValidUntilBefore` (`status == ACTIVE && validUntil < day`) needs the composite index
   `(status ASC, validUntil ASC)`. Indexes are managed by Terraform in the IaC repo.
7. **Merchant list is loaded through an admin endpoint**: `PUT /merchants/{cnpj}` (idempotent,
   204), use case `RegisterMerchantUseCase`, port method `MerchantList.add(Merchant)`. The new
   domain record `Merchant` validates the CNPJ format (14 digits) → 422 otherwise. Same HTTP Basic
   credentials as the rest of the API for now.
8. **Tests:** `FirestoreOptInRepositoryTest extends OptInRepositoryContractTest` against the
   Firestore emulator (Testcontainers `FirestoreEmulatorContainer`), plus R1 tests (duplicate,
   re-grant after revoke, concurrent grants). `FirestoreProfileApplicationTests` boots the whole
   app with the `firestore` profile and asserts that no DataSource exists.
9. **Infrastructure (IaC repo):** Firestore database `(default)` and indexes, `roles/datastore.user`
   for the `optin` Kubernetes service account. Cloud SQL becomes optional (`sql.enabled: false`).
   The deployment loses the Cloud SQL proxy sidecar and the DB password secret. Cloud NAT is removed
   (`network.nat_enabled: false`): images come from Artifact Registry over Private Google Access,
   and the CD smoke test uses `kubectl port-forward` from the runner instead of a public curl image.

## Alternatives considered

- **Keep Cloud SQL:** strongest consistency and SQL auditability, ~$10/month + proxy + private
  networking. Kept as an option in the IaC repo; the Postgres adapter stays.
- **Integer ids with a hi/lo counter document:** keeps the API unchanged, but adds a hot document
  (Firestore sustains about one write per second per document) and allocation code. Rejected in
  review in favour of UUIDs.
- **Spanner:** relational and horizontally scalable, but ~$66/month after the 90-day trial.
- **Replace the Postgres adapter:** loses the local Podman workflow and the comparison that makes
  this exercise valuable.

## Consequences

- **Breaking API change:** `id` is now a UUID string; clients that stored integer ids must migrate.
  Existing PostgreSQL rows receive new random ids in V3 (dev data only).
- Two adapters to maintain; the contract test keeps them equivalent.
- No schema or migrations in Firestore: shape and validation live in the domain and the adapter.
- No ad-hoc SQL for audits: reporting goes through Pub/Sub → BigQuery (already provisioned).
  Fine for a learning environment; a real regulated registry would weigh this heavily.
- Phase 3 (outbox) on Firestore: the event document is written in the same transaction as the
  opt-in and relayed to Pub/Sub; revisit the Kafka choice in that phase's ADR.
- Exceeding 20,000 writes/day (e.g. a 100k batch import in one day) is billed at standard rates.
- Enabling a domain with ExternalDNS later requires Cloud NAT again (its image comes from
  registry.k8s.io); the IaC repo refuses `dns.domain` without `network.nat_enabled: true`.
