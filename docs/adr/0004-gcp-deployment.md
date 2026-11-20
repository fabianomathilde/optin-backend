# 0004 — Deployment on GCP: GKE Autopilot, Cloud SQL and keyless CI/CD

- **Status:** Proposed (awaiting human review and acceptance)
- **Date:** 2026-11-20
- **Phase:** cross-cutting (infrastructure)

## Context

The service has a working Postgres adapter (ADR 0001) and needs a real, repeatable environment
to run in. Goals: practice on a production-like GCP setup (Kubernetes, managed database,
external DNS, Pub/Sub, BigQuery), keep the cost of a training project low, and keep the
infrastructure reproducible so environments can be destroyed and recreated at will.

Two GCP projects: `registry-institution-dev` (development) and `registry-institution` (prod).
Infrastructure lives in a separate repository, `registry-institution-iac` (Terraform).

## Decision

1. **Runtime: GKE Autopilot**, one regional cluster per project, private nodes.
   The app is deployed with Kustomize (`deploy/k8s`, base + `dev`/`prod` overlays).
2. **Database: Cloud SQL for PostgreSQL 17, private IP only.** The schema stays owned by Flyway.
   The app connects through the **Cloud SQL Auth Proxy as a native sidecar**, listening on
   `127.0.0.1:5432`, so the JDBC URL is the same as locally and the persistence adapter
   doesn't change.
3. **Secrets: Secret Manager**, mounted as files by the GKE Secret Manager add-on and read with
   Spring's `configtree` import. The file names are property names
   (`spring.datasource.password`, `spring.security.user.password`). No secret in Git, in the
   image or in Kubernetes Secrets.
4. **Identity: Workload Identity Federation everywhere, no service-account keys.**
   - Pods: IAM roles are granted to the Kubernetes ServiceAccount `optin/optin` directly
     (principal identifiers).
   - GitHub Actions: OIDC federation; this repo can only impersonate `optin-deployer`
     (push images, deploy to GKE). Terraform runs under a separate `terraform-ci` account that
     only the IaC repo can use.
5. **Health endpoints** (`/actuator/health/**`) are public; everything else keeps HTTP Basic.
   Only `health` is exposed over HTTP.
6. **Delivery: one CI, one CD with two stages, build once, deploy many.**
   `ci.yaml` runs `./mvnw verify` and a Docker build on every branch and pull request; on
   `develop` and `main` it also pushes the image, tagged with the commit SHA (immutable tags).
   `cd.yaml` runs after a successful CI push: `develop` deploys to dev; `main` deploys to dev and
   then, after a manual approval on the GitHub `prod` environment, promotes the same digest to prod.
7. **No public endpoint by default.** The app is reached with `kubectl port-forward` while
   the project is a learning environment; a public load balancer is billed per hour even when idle.
   An optional Kustomize component adds a GKE Ingress on a reserved static IP (container-native
   load balancing); with a domain, ExternalDNS publishes the hostname in Cloud DNS and a
   Google-managed certificate serves HTTPS.
8. **Start/stop instead of destroy.** `scripts/env.sh` (IaC repo) scales the app to zero and
   stops Cloud SQL; Terraform ignores the database's on/off state after creating it.

## Alternatives considered

- **Cloud Run** instead of GKE: cheaper (scale to zero) and simpler, but no Kubernetes
  practice, which is a goal of this project. The Dockerfile works on both, so the choice is
  reversible.
- **Cloud SQL Java Connector** instead of the proxy sidecar: one less container, but it adds a
  GCP dependency to the persistence adapter's runtime and changes the JDBC URL. The sidecar
  keeps the adapter infrastructure-agnostic.
- **Kubernetes Secrets synced from Secret Manager** (External Secrets Operator): one more
  controller to run; the managed add-on covers the need.
- **Spanner** as the database: the current schema relies on a Postgres sequence and a partial
  unique index (ADR 0001). A Spanner adapter would be a new driven adapter with its own schema
  and its own ADR. The IaC repo can create a Spanner free-trial instance when that work starts.

## Consequences

- `pom.xml` gains `spring-boot-starter-actuator`; `SecurityConfiguration` permits health.
- The app needs about 768 MiB per pod; Autopilot bills requested resources, so dev runs a single
  replica and the database uses the smallest tier (`db-f1-micro`, no SLA).
- Hikari pool size is 5 per pod: `pool size x replicas` must stay below the instance's
  connection limit.
- Domain events (outbox, phase 3) can publish to the `optin-events` Pub/Sub topic already
  created by the IaC repo; a BigQuery subscription stores them for reporting, so the Kafka
  choice in CLAUDE.md phase 3 should be revisited in that phase's ADR.
- Rolling back is redeploying an older digest; Flyway migrations stay append-only, so a rollback
  must be compatible with the newer schema (expand/contract).
