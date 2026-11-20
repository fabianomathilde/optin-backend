# 0002 — Input ports and REST adapter for opt-ins

- **Status:** Proposed (awaiting human review and acceptance)
- **Date:** 2026-11-19
- **Phase:** 1 (Domain + use cases + REST adapter)

## Context

The first use cases need a driving (input) side so that clients can grant, list and revoke opt-ins
over HTTP. CLAUDE.md requires:

- driving ports in `application/port/in/` (use case interfaces and command records);
- REST controllers and DTOs in `adapters/in/rest/`, never leaking into the core;
- a contract-first HTTP API: `src/main/resources/optin-api.yaml` is written before the controller.

The required operations are:

1. create a new opt-in;
2. list all opt-ins of a given grantee, or of a given merchant;
3. revoke a given opt-in.

## Decision

### Input ports (`application/port/in/`)

| Port                    | Method                                             | Input                      | Output       |
|-------------------------|----------------------------------------------------|----------------------------|--------------|
| `CreateOptInUseCase`    | `OptIn create(CreateOptInCommand)`                 | command record             | created opt-in |
| `ListOptInsUseCase`     | `List<OptIn> listByGrantee(String grantee, Optional<OptInStatus> status)`   | grantee document, optional status | opt-ins |
|                         | `List<OptIn> listByMerchant(String merchant, Optional<OptInStatus> status)` | merchant document, optional status | opt-ins |
| `RevokeOptInUseCase`    | `OptIn revoke(RevokeOptInCommand)`                 | command record             | revoked opt-in |

Command records:

- `CreateOptInCommand(String merchant, String grantee, Frequency frequency, LocalDate startDate, LocalDate validUntil)`
- `RevokeOptInCommand(int optInId)`

The ports use the domain types `OptIn`, `Frequency` (`DAILY`, `WEEKLY`, `MONTHLY`) and `OptInStatus`
(`ACTIVE`, `REVOKED`). Ports return domain objects, not DTOs.

### REST adapter (`adapters/in/rest/`)

| Method | Path                                   | Use case            | Success |
|--------|----------------------------------------|---------------------|---------|
| POST   | `/opt-ins`                             | create              | 201 + opt-in |
| GET    | `/grantees/{grantee}/opt-ins?status=`  | listByGrantee       | 200 + array |
| GET    | `/merchants/{merchant}/opt-ins?status=`| listByMerchant      | 200 + array |
| POST   | `/opt-ins/{id}/revoke`                 | revoke              | 200 + opt-in |

- Two separate list paths instead of one with optional query parameters. The path makes the filter
  explicit and avoids the "both or neither given" case.
- Revoke is a state change, so it is a `POST` on a sub-resource, not a `DELETE`. The row is kept
  (see ADR 0001).
- The controller depends only on the input ports. It maps request DTOs to commands and domain objects
  to response DTOs (`CreateOptInRequest`, `OptInResponse`).
- JSON uses camelCase (`startDate`, `validUntil`, `revokedAt`).
- The contract is in `src/main/resources/optin-api.yaml` and is the source of truth for the HTTP shape.

## Alternatives considered

- **One list endpoint with `?grantee=` or `?merchant=` query parameters.** Rejected: needs validation
  of "exactly one filter" and is less explicit in the contract.
- **One port with all operations (`OptInService`).** Rejected: CLAUDE.md asks for use case interfaces.
  Separate interfaces keep each controller dependency small.
- **`DELETE /opt-ins/{id}` for revoke.** Rejected: revoke keeps the record and changes its status.
- **Returning DTOs from the ports.** Rejected: the core would know about HTTP shapes.
- **Validating `Cnpj` and the date rules in the controller.** Rejected: business rules live in the domain.
  The controller only checks shape (types and formats in the contract).

## Consequences

- The ports compile now, but **no service implements them yet**. Until the use case services and their
  in-memory adapters exist (next steps of phase 1), the application cannot start: Spring finds no bean
  for the ports. Controller tests use mocks for the ports.
- Business errors (duplicate active opt-in, invalid date, unknown id, already revoked) are not mapped to
  HTTP codes yet. They need domain exceptions and a `@RestControllerAdvice`. The contract lists 404 and
  409 for those cases; they are not produced until that mapping exists.
- Input validation is limited to what the types give (JSON types, enum values). Format rules (CNPJ
  digits, date rules) are added with the domain value objects.
- Spring Security is on the classpath, so REST calls need authentication once the app runs. Security
  rules are not part of this ADR.
- The ArchUnit test required by CLAUDE.md is not in place yet. It should be added before this phase is closed.

## Decisions on former open questions

- The revoke endpoint returns the revoked opt-in with 200 (not 204).
- The list endpoints accept an optional `status` query parameter (`ACTIVE` or `REVOKED`). Omitting it returns all statuses. An unknown value is rejected by Spring with 400.

## Open questions

None for this ADR.
