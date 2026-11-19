# Roadmap (post Issue 7)

Issues 1-7 are the core, all merged. Items below are optional, ordered by dependency and value. Each becomes its own branch `feature/<n>-<slug>` with `Closes #<n>` in the PR body, following the workflow in `CLAUDE.md`.

## Core hardening (cheap, supports NFR-14 and NFR-17)

1. **OpenAPI documentation depth**
   - `@Operation` summaries and `@Schema` descriptions on request and response records.
   - Documented 400, 404, 409 and 503 responses using the problem-details types (`validation-error`, `machine-busy`, and so on).
   - Example request and response bodies for the play and refill endpoints.
   - Test that asserts the error responses are documented.
2. **Actuator metrics**: expose health and metrics only; no sensitive endpoints.
3. **Idempotency keys on `POST /plays`**: a retried request with the same key does not play twice. Needs a key store and a decision on key lifetime.

## Issue 8 (stretch): authentication and authorisation

4. **JWT resource server (Spring Security)**: the app validates bearer tokens. Start with a simple token issuer for local use.
5. **RBAC**: roles `OPERATOR` (configure, refill) and `PLAYER` (play only), enforced with `@PreAuthorize`. Tests for each role.
6. **Keycloak (OIDC)** in Docker Compose, with realm setup and docs. Only if time allows. Adds a Testcontainers Keycloak test.

## Optional after Issue 8

7. **Rate limiting** (Bucket4j or gateway). Complements the NFR-17 input limits. Keying per principal is easier once step 4 is in.
8. **Resilience4j**: timeouts and circuit breaking around the database call.
9. **Pagination on play history** (API-05, optional).

## Issue 9 (deployment, after the core is merged)

10. **Secrets**: AWS Secrets Manager or Parameter Store instead of environment variables. Needed before any cloud deploy, and before the JWT signing config is moved off local files.
11. **RDS MySQL 8**: change connection settings only; Flyway migrations already fit.
12. **Backups and restore**: RDS automated backups plus a documented, tested restore.
13. **Infrastructure as code**: Terraform or AWS CDK for RDS, ECR and the runtime.
14. **Runtime**: App Runner or ECS Fargate (smaller). EKS is larger and costly to run; treat it as an interview talking point only.

## Notes

- Stop after any step with a working merged core. Items 4-6 and 10-14 are independent of each other beyond the order shown.
- Verification evidence for each item goes in `docs/verification.md`.
