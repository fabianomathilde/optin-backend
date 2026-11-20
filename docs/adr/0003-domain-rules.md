# 0003 — Domain rules for opt-ins

- **Status:** Proposed (awaiting human review and acceptance)
- **Date:** 2026-11-19
- **Phase:** 1 (Domain + use cases)

## Context

CLAUDE.md lists the core invariants of an opt-in. The domain work adds new rules: a merchant must be
on the merchant list, the validity period is capped at one year from the start date, the start date
cannot be in the past, and expired opt-ins must be revoked by a daily check. This ADR records every
rule, where it is enforced, and how it is tested.

## Decision

### Rules

| ID  | Rule | Enforced in | Error on violation |
|-----|------|-------------|--------------------|
| R1  | A grantee has at most one ACTIVE opt-in per merchant. | `OptInService.create` checks `OptInRepository.findActive`. DB partial unique index `uq_opt_in_active_pair` is the backstop (ADR 0001). | `DuplicateActiveOptInException` → 409 |
| R2  | The merchant must be ACTIVE in the merchant list. | `OptInService.create` checks the `MerchantList` out port. | `MerchantNotActiveException` → 422 |
| R3  | On grant, `validUntil` must be after today. | `OptIn.grant`. | `InvalidValidityException` → 422 |
| R4  | On grant, `validUntil` must not be more than one year after `startDate`. | `OptIn.grant`. | `InvalidValidityException` → 422 |
| R5  | On grant, `startDate` must not be after `validUntil` (same as the DB check). | `OptIn.grant`. | `InvalidValidityException` → 422 |
| R6  | A revoked opt-in cannot be revoked again and cannot be reactivated. | `OptIn.revoke`. There is no reactivation operation. | `OptInAlreadyRevokedException` → 409 |
| R7  | An opt-in is effective on day D only if it is ACTIVE and D ≤ `validUntil`. | `OptIn.isEffectiveOn`. | — (query rule) |
| R8  | When `validUntil` has passed (today is after `validUntil`), the opt-in is revoked. The check runs once a day. | `OptInService.revokeExpired`, exposed by `CheckOptInValidityUseCase`. | — |
| R9  | Revoking an unknown id fails. | `OptInService.revoke`. | `OptInNotFoundException` → 404 |
| R10 | On grant, `startDate` must not be before today. | `OptIn.grant`. | `InvalidValidityException` → 422 |

### Interpretations

- **R4 base date:** the one-year cap is counted from `startDate`. A grant can cover at most one year
  from the day it starts.
- **R10 and today:** a grant can start today or later. "Today" is the date of the injected clock.
- **R8 timing:** On the `validUntil` day the opt-in is still effective (R7). It is revoked by the
  first daily check after that day. `revokedAt` is the instant of the check.
- **Time source:** All dates come from a `java.time.Clock` injected into the service. It is a JDK type,
  so the core has no framework dependency. Tests use `Clock.fixed`.

### Ports

- Out port `OptInRepository` (domain-language finders, no JPA names):
  `save`, `findById`, `findActive(merchant, grantee)`, `findByGrantee(grantee, status)`,
  `findByMerchant(merchant, status)`, `findActiveValidUntilBefore(day)`.
- Out port `MerchantList`: `isActive(merchant)`. The table behind it is defined later, so there is no adapter yet.
- In port `CheckOptInValidityUseCase`: `int revokeExpired()`. Returns the number of opt-ins revoked.

### HTTP adapter for the daily check

- `POST /opt-ins/validity-checks` → 200 `{ "revokedCount": n }`.
- The endpoint is called once a day by an external scheduler (cron job or similar). The application does
  not schedule itself, so runs are visible in the scheduler and the endpoint can be called by hand.
- The endpoint is protected by the same security as the other endpoints. The scheduler needs credentials.

### Error mapping

A `@RestControllerAdvice` maps domain exceptions to the status codes in `optin-api.yaml`, with body
`{ "message": "..." }`. This closes the gap noted in ADR 0002.

## Alternatives considered

- **Check R1 only in the database.** Rejected: the rule must be a domain rule with a clear error, and the
  index alone gives a generic constraint violation. The index stays as a race-condition backstop.
- **Count R4 from today.** Rejected: the start date is the period's beginning, so the cap belongs to it.
- **Revoke on the `validUntil` day.** Rejected: that contradicts R7, which keeps the opt-in effective on that day.
- **Run the daily check inside the application with `@Scheduled`.** Rejected for now: the request asks for
  an HTTP adapter called by a service. A scheduled task can be added later.
- **Put expiry logic in the controller or a batch job.** Rejected: business rules belong in the core.

## Consequences

- Every rule above has a unit test with in-memory fakes (no Spring context), as CLAUDE.md requires.
- Revocation is a state change, so CLAUDE.md requires a domain event. **No event port exists yet** (phase 3).
  Revoke and grant do not publish events in this step.
- The application does not start yet. Beans for `OptInRepository` (phase 2 Postgres adapter) and
  `MerchantList` (adapter for the merchant table) do not exist. Service wiring in `config/` is also pending.
- The merchant table adapter depends on the data model the user will provide.
- The ArchUnit test required by CLAUDE.md is still not in place.

## Open questions

None for this ADR.
