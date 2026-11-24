# Deploying optin-backend

Everything in GCP and every cluster-level Kubernetes object is created by the
**registry-institution-iac** repository (`envs/<env>/*.yaml` there): namespace `optin-backend`
with quota and deployer RBAC, registry `optin-backend`, secret `optin-backend-api-password`, topic
`optin-backend-events`, and the identities in `service_accounts.yaml`:

| Identity | Used by | Access |
|---|---|---|
| `sa-optin-backend-ci` (GSA) | this repo's GitHub Actions | push to registry `optin-backend`, deploy into namespace `optin-backend` |
| `sa-optin-backend` (GSA + KSA) | the pods (`serviceAccountName: sa-optin-backend`) | Firestore `(default)` writer, secret reader, topic publisher |

This folder only describes the app: it creates no namespace, service account, role or IAM, and the
pipeline's identity isn't allowed to.

In GCP the app runs with the **`firestore` profile** (ADR 0005): the Firestore adapter replaces
PostgreSQL, and there is no Cloud SQL proxy or database password. PostgreSQL remains the default
profile for local development.

```
deploy/k8s/
  base/                 SecretProviderClass (API password), Deployment,
                        Service, BackendConfig  (no public endpoint)
  components/public-ingress/ optional public load balancer on the reserved static IP
  components/https-domain/   optional hostname (ExternalDNS) + Google-managed certificate
  overlays/dev/         registry-institution-dev
  overlays/prod/        registry-institution-prd (2 replicas + PodDisruptionBudget)
```

## Pipeline: `ci.yaml` builds, `cd.yaml` deploys (dev, then prod)

```
.github/workflows/
  ci.yaml   every push (any branch) and every PR: ./mvnw verify + docker build
            develop / main: also pushes optin-backend:<commit sha> to the dev registry
  cd.yaml   after a successful CI push on develop or main:
              job dev  : deploy that image to dev + smoke test
              job prod : main only, after approval: same digest copied to the prod registry, deployed
            manual run: redeploy / roll back the image of any commit SHA
```

| Event | ci.yaml | cd.yaml dev | cd.yaml prod |
|---|---|---|---|
| push to a feature/release/hotfix branch, any PR | verify + build | – | – |
| push to `develop` (PR merged) | verify + build + push | ✓ | – |
| push to `main` (release/hotfix merged) | verify + build + push | ✓ | ✓ after approval |

- **Build once, deploy many:** prod receives the exact digest that passed dev.
- `cd.yaml` is triggered by `workflow_run`, which always uses the `cd.yaml` on `main`:
  changes to it take effect once merged to `main`.

## GitHub setup (once)

1. Branches: `main` and `develop` (from `main`). Protect both: pull request required and the
   `ci / verify` check required.
2. Settings → Environments:
   - `dev`: variables from the IaC repo (`GCP_PROJECT_ID`, `GCP_REGION`, `WIF_PROVIDER`,
     `DEPLOYER_SA` = `sa-optin-backend-ci@…`, `GKE_CLUSTER`, `ARTIFACT_REPOSITORY`):
     `./scripts/tf.sh dev identities output -json github_variables | jq '."optin-backend-ci"'`
   - `prod`: the same command with `prod`, plus
     `DEV_ARTIFACT_REPOSITORY` = `us-central1-docker.pkg.dev/registry-institution-dev/optin-backend`;
     *Required reviewers*: you; *Deployment branches*: `main` only.
3. In the IaC repo, uncomment the prod deployer under `external_readers` in
   `envs/dev/registries.yaml`, so prod can pull the dev image it promotes.

## Running locally with Podman (no GCP needed)

Default profile, PostgreSQL 17 from `compose.yaml`; the app's default settings already point to
it (`localhost:5432`, user `optin`).

```bash
podman machine start                       # once per boot on macOS
podman compose up -d postgres              # Postgres 17 with a persistent volume
./mvnw spring-boot:run                     # Flyway migrates, API on :8080
curl http://localhost:8080/actuator/health  # public; other endpoints use Basic auth:
                                           # user "user", password printed in the startup log

podman compose down                        # stop, keep data
podman compose down -v                     # stop and delete data
```

Same app against the **Firestore emulator** (what GCP runs):

```bash
podman compose up -d firestore
SPRING_PROFILES_ACTIVE=firestore OPTIN_FIRESTORE_EMULATOR_HOST=localhost:8081 ./mvnw spring-boot:run
```

Integration tests use Testcontainers (PostgreSQL and the Firestore emulator). With Podman instead of Docker, export once per shell:

```bash
export DOCKER_HOST="unix://$(podman machine inspect --format '{{.ConnectionInfo.PodmanSocket.Path}}')"
export TESTCONTAINERS_RYUK_DISABLED=true
./mvnw verify
```

## Manual deploy / debugging (from your Mac)

```bash
gcloud container clusters get-credentials registry-institution \
  --region us-central1 --project registry-institution-dev

kubectl kustomize deploy/k8s/overlays/dev          # render without applying
kubectl -n optin-backend get pods
kubectl -n optin-backend logs deploy/optin-backend

# Call the API through a tunnel (no public IP needed)
kubectl -n optin-backend port-forward svc/optin-backend 8080:80 &
PASS=$(gcloud secrets versions access latest --secret optin-backend-api-password \
  --project registry-institution-dev)
curl http://localhost:8080/actuator/health

# Put a merchant on the merchant list (idempotent), then grant an opt-in for it
curl -u "optin-api:${PASS}" -X PUT "http://localhost:8080/merchants/11222333000181"
curl -u "optin-api:${PASS}" -H 'Content-Type: application/json' -X POST http://localhost:8080/opt-ins \
  -d '{"merchant":"11222333000181","grantee":"00360305000104","frequency":"MONTHLY",
       "startDate":"2026-12-01","validUntil":"2027-06-01"}'
curl -u "optin-api:${PASS}" "http://localhost:8080/grantees/00360305000104/opt-ins"
```

Start/stop the environment to save credit (IaC repo): `./scripts/env.sh dev stop|start|status`.

## Public access (optional)

1. IaC repo, `envs/dev/network.yaml`: `ingress.enabled: true` and `optin-backend` under
   `ingress.ips`, merged (reserves `optin-backend-ingress-ip`).
2. Here: uncomment `../../components/public-ingress` in `overlays/dev/kustomization.yaml`.
3. The first load balancer takes 5–10 minutes before the IP answers (~$18/month while it exists).

## Values that must match the IaC repo

| Here | IaC |
|---|---|
| namespace `optin-backend` | `kubernetes.yaml` → `namespaces` |
| `serviceAccountName: sa-optin-backend` | `service_accounts.yaml` → `optin-backend` (type workload) |
| `GOOGLE_CLOUD_PROJECT` in the overlay config map | `project.yaml` → `project_id` |
| project + secret `optin-backend-api-password` in the SecretProviderClass patch | `project_id`, `secrets.yaml` |
| public-ingress `global-static-ip-name: optin-backend-ingress-ip` | `network.yaml` → `ingress.ips.optin-backend` |
| image `newName` in the overlay | `ARTIFACT_REPOSITORY` (identities output `github_variables`) + `/optin-backend` |
