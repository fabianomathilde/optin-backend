# CLAUDE.md — Receivables Opt-in & Agenda Service

Training project for hexagonal architecture (Ports & Adapters) in a fintech domain.
The code is AI-written; the architectural decisions are owned by the human and recorded as ADRs.

## Domain
A merchant (CNPJ) grants a financial institution an **opt-in** to view its receivables
schedule (agenda). An opt-in has a validity date and can be revoked (opt-out).
Every state change is published as a domain event.

Core invariants:
- Only one ACTIVE opt-in per (merchant, grantee).
- `validUntil` must be after today when granting.
- A revoked opt-in cannot be revoked again or reactivated.
- An opt-in is effective on date D only if ACTIVE and D <= validUntil.

## Architecture rules (non-negotiable)
Base package: `com.example.optin` (adjust to the project's actual group id).

```
domain/                  pure Java. No Spring, no JPA, no Jackson, no Lombok annotations.
application/port/in/     driving ports (use case interfaces + command records)
application/port/out/    driven ports (repository, event publisher, clock, external registry)
application/service/     use case implementations; orchestration only, no business rules
adapters/in/rest/        Spring MVC controllers + request/response DTOs
adapters/in/batch/       file ingestion (phase 4)
adapters/out/persistence/ JPA entities, Spring Data repos, mappers
adapters/out/messaging/  Kafka publisher, outbox relay (phase 3)
adapters/out/registry/   external registry client + anti-corruption layer (phase 5)
config/                  Spring wiring (@Configuration beans for services)
```

- Dependencies point inward only: adapters -> application -> domain.
- `domain` and `application` must not import `org.springframework..`, `jakarta.persistence..`,
  `com.fasterxml..` or anything under `adapters`.
- Application services are plain classes, instantiated in `config/` as `@Bean`s
  (no `@Service`/`@Component` in the core).
- Business rules live in domain objects (rich model). If a service method accumulates `if`s
  about opt-in state, move that logic into the aggregate.
- Ports are named in domain language (`findActive(merchant, grantee)`), never JPA-style
  (`findByMerchantAndGranteeAndStatus`).
- JPA entities never leave `adapters/out/persistence`. REST DTOs never leave `adapters/in/rest`.
- Use Java records for value objects, commands and events. Value objects validate in the
  compact constructor (e.g. `Cnpj` validates format and check digits).
- Time comes from the `Clock` port, never `LocalDate.now()` in the core.

## Testing rules
- Domain and application are tested with plain JUnit 5 + AssertJ and in-memory fakes
  (`InMemoryOptInRepository`, `RecordingEventPublisher`, fixed clock). No Spring context.
- Adapters are tested with Testcontainers (Postgres, Kafka) or `@WebMvcTest`.
- `ArchitectureTest` uses ArchUnit to enforce every rule in the section above.
  It must exist from phase 1 and must stay green.
- Every invariant listed under "Domain" has at least one test.

## Phases (do one at a time; stop and wait for review after each)
1. Domain + grant/revoke use cases + REST adapter + in-memory adapters + ArchUnit test.
2. Postgres adapter (Flyway migrations). Handle concurrent grants for the same pair
   (domain check + DB partial unique index; map the DB violation to a domain error).
3. Reliable event publishing with a transactional outbox + relay to Kafka.
4. Batch ingestion of 100k opt-ins from CSV reusing the same use case.
5. External registry check before granting: anti-corruption layer, timeouts, circuit breaker
   (Resilience4j). Separate domain errors from infrastructure errors.
6. Harden fitness functions (ArchUnit, coverage on domain, mutation testing optional).
7. Extract "Agenda" into its own service; document what moved and what broke.

## Working agreement for the AI
- Before coding a phase, propose the design (ports, classes, trade-offs) and wait for approval.
- At the end of a phase, draft an ADR in `docs/adr/NNNN-title.md` (Context, Decision,
  Alternatives considered, Consequences). The human edits and accepts it.
- Small commits, one concern each. Run `./mvnw verify` (or `./gradlew check`) before saying done.
- Never weaken or delete an ArchUnit rule to make a build pass; flag it instead.
- When a choice is debatable (transaction boundary, port granularity, mapping layers),
  stop and present the options instead of picking silently.

## Sources of truth
- HTTP contract: `src/main/resources/openapi/optin-api.yaml` (contract-first; never change
  the controller without changing the YAML first).
- Database schema: `src/main/resources/db/migration/` (Flyway, append-only).
- Current phase requirements: `docs/specs/phase-N.md`.
- Decisions: `docs/adr/`. Read related ADRs before changing a design.

@docs/domain.md