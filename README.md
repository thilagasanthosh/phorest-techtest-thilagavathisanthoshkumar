# phorest-techtest-thilagavathisanthoshkumar

Fruit Machine Service: a Spring Boot REST API for the take-home brief (`docs/candidate-brief-fruit-machine-take-home-1-2-.pdf`). It configures a machine (slots, colours, k, play cost, float, float policy), plays it, refills it, and reports its state. Machine state is stored in MySQL.

Brief parts covered: core spin and jackpot (Part 1), payouts and free-play shortfall (Part 2), variable slots, colours and k (Part 3), REST service (Part 4). Optional extras done: Docker Compose, CI, persistence, Swagger, metrics.

## 1. How to run

### Prerequisites

- JDK 21 (the bundled formatter fails on newer JDKs)
- Docker, running (tests and the dev launcher use MySQL through Testcontainers)
- Optional: Newman for the command-line Postman run (`npm i -g newman`)

### One-time setup

```
git clone <repo-url>
cd phorest-techtest-thilagavathisanthoshkumar
git config core.hooksPath .githooks   # pre-commit formatter hook
./mvnw verify                          # build + all tests + format check
```

### Option 0: Docker Compose (MySQL, app, Prometheus, Grafana)

```
docker compose --env-file .env.example up -d --build   # local only: uses the example passwords
curl http://localhost:8080/actuator/health
docker compose --env-file .env.example down -v   # stop and delete the data
```

Compose starts MySQL 8.0 and the app. The app connects as `fruitmachine_app`, which has rights on the `fruitmachine` schema only. `.env` is gitignored; never commit it.

Metrics, once the app is UP:

```
curl http://localhost:8080/actuator/metrics/fruitmachine.plays          # Actuator JSON
curl http://localhost:8080/actuator/prometheus | grep fruitmachine_      # Prometheus format
```

Grafana is at http://localhost:3000 (login `admin`, password `GRAFANA_ADMIN_PASSWORD` from `.env`), dashboard "Fruit Machine Service". Prometheus is at http://localhost:9090.

![Grafana "Fruit Machine Service" dashboard](docs/images/grafana-dashboard.png)

### Option A: dev launcher with its own MySQL

Run `src/test/java/com/thilaga/fruitmachine/TestFruitmachineApplication.java` from your IDE (its `main` method). Docker must be running. The launcher starts a throwaway MySQL container; data is lost when it stops.

### Option B: your own MySQL

Create an empty database, then point the app at it (no datasource is set in `application.properties`):

```
export SPRING_DATASOURCE_URL='jdbc:mysql://localhost:3306/fruitmachine'
export SPRING_DATASOURCE_USERNAME=<user>
export SPRING_DATASOURCE_PASSWORD=<password>
./mvnw spring-boot:run
```

Flyway creates the tables on first start. The API listens on `http://localhost:8080`.

## 2. How to test

```
./mvnw verify                                          # everything, incl. format check
./mvnw test -Dtest=MachineReadApiTest                  # one class
./mvnw test -Dtest=MachineInteractionApiTest#someName  # one method
./mvnw fmt:format                                      # fix formatting
```

Test layers:
- **Domain:** pure Java, no Spring (`MachineTest`, `PayoutSettlerTest`, `PrizeEvaluatorTest`, policy and rule tests).
- **Application:** `MachineServiceTest`, `MachineHistoryTest` (Spring + Testcontainers MySQL).
- **Infrastructure:** `SchemaConstraintsTest` (CHECK constraints), `JpaMachineRepositoryTest` (lock timeout).
- **API:** `MachineInteractionApiTest` and siblings (MockMvc, real MySQL).
- **Postman:** the collection below, run with Newman.

### Test results in CI

Each CI run (`.github/workflows/ci.yml`) publishes a test summary:

- **PR comment:** on pull requests, one comment per PR (updated on each push) with a table per test class: Tests, Failures, Errors, Skipped.
- **Run summary:** the same table on the Actions run's **Summary** page.
- **Artifact:** the raw surefire XML reports, in the `test-reports` artifact.

A class with 0 in Failures, Errors and Skipped passed. The `build` job's status is the overall result. If the build fails before tests run, the table is empty or missing rows.

### Postman collection

The collection is at `postman/fruitmachine-api.postman_collection.json`. It creates its own data, so you do not load any test data.

- **Newman:** with the app running, `newman run postman/fruitmachine-api.postman_collection.json --env-var baseUrl=http://localhost:8080`
- **Postman app:** File → Import the collection, check `baseUrl` is `http://localhost:8080`, run folders top to bottom.
- Run folders in order. A folder run alone fails because its variables are empty.
- Not covered over HTTP: AC-25 and AC-27 (free-play credit needs forced spins, covered by system tests in Issue 7) and 503 `machine-busy` / AC-32 (needs concurrent load, covered in Issue 5).

## 3. API

Swagger UI: http://localhost:8080/swagger-ui/index.html · OpenAPI JSON: http://localhost:8080/v3/api-docs

| Method | Path | Purpose |
|---|---|---|
| POST | `/api/v1/machines` | Configure a machine (201 + `Location`) |
| GET | `/api/v1/machines/{id}` | Read machine state |
| POST | `/api/v1/machines/{id}/plays` | Play (`{"useFreePlay": false}`) |
| POST | `/api/v1/machines/{id}/refills` | Operator refill (`{"amountPence": 500}`) |

Errors use RFC 7807 problem details (`application/problem+json`). Types: `validation-error` (400), `machine-not-found` (404), `machine-needs-refill` (409), `no-free-plays` (409), `machine-busy` (503 with `Retry-After`).

## 4. Key engineering decisions

Full list D1–D11 with the reasoning is in `docs/gitHub_issue_bodies.md`. The ones that shape the code:

- **D1** Only the highest prize pays (jackpot > full house > small prize). One pass over the slots, so evaluation stays O(slots).
- **D2** The stake joins the float before evaluation.
- **D3** Free plays = ceil(shortfall ÷ play cost). The float is paid down to 0.
- **D4 / D5** Money is whole pence (`BIGINT`), never floating point. Half of an odd float rounds down.
- **D6** A full house is impossible when slots > colours, so it is never awarded then.
- **D7 / D8** A free play adds no stake and uses one credit. Credits are a balance on the machine; there are no player accounts.
- **D9** A jackpot sets the float to 0 and never creates free plays.
- **D10** Policy MIN_RESERVE (the default) refuses plays while float < 5 × play cost. Policy NONE accepts every play.
- **D11** Refills are operator actions with their own audit history.

Design shape: `domain` is pure Java (game rules, `FloatPolicy`, `PrizeRule`s, `PayoutSettler`); `application` holds `MachineService` with one transaction per play and a pessimistic lock; `infrastructure` holds JPA, Flyway and the spin generator; `api` holds the controllers and error mapping. Prize rules are a list (`PrizeEvaluator`), so a new rule is a new class, not an edit to existing ones.

## 5. Trade-offs

- **Free plays never arise under MIN_RESERVE.** The reserve guarantees every prize is paid in full, so the shortfall rule (and so the credits) never triggers. Free plays are testable on NONE machines only. Adding credits to MIN_RESERVE needs a spec decision (for example, credits granted on refill).
- **NONE can loop credits.** On an empty NONE machine each free play nets +4 credits (it wins a small prize for 5 credits and uses 1), with no money in. The brief's rule allows it; a cap or a rule against earning from an empty float would close it.
- **App DB user has DDL rights.** Flyway migrates at startup, so `fruitmachine_app` can create and alter tables. A separate migrator user is the fix (see below).
- **Counters are incremented before commit returns.** If the commit fails after the save, the metrics are ahead of the DB. Acceptable for observability; money is checked in the DB.
- **Docker build downloads Maven deps on every build.** No BuildKit cache mount, so builds are slower.
- **CI depends on Maven Central at run time.** The Maven wrapper downloads the distribution on each run; a transient network failure fails the job (seen twice; a rerun passed).

## 6. Deliberately left out, and what I would do next

Out of scope for this version, with reasons recorded in the plans:

- **Separate migrator DB user** (least privilege for Flyway). Needs an extra service; no acceptance criterion.
- **Idempotency-Key header** for plays. Design 5.1 says "later".
- **Play history and list-machines endpoints** (API-05, API-06). Optional in design 5.2; no use case.
- **404/405/415 and malformed path ids mapped to their own statuses.** Currently they can fall into the catch-all 500. Open decision in `docs/plans/US-04-rest-api-plan.md` (section 9).
- **Property-based tests** (jqwik) and **load tests beyond 200 plays**. No acceptance criterion; would add dependencies.
- **Distributed tracing** (Micrometer Tracing / OpenTelemetry). The per-play log line covers NFR-14; ask before adding a dependency.

Next steps with more time, in order: fix the 404/405/415 mapping; decide the free-play rule for MIN_RESERVE and the NONE loop; add a migrator DB user; cache the Maven distribution in CI.

## 7. Where AI was used

- Used for: writing the issue plans and specs in `docs/`, drafting the code per issue, writing and reviewing tests, and the Compose, CI and Grafana setup.
- Disagreed with or reworked: the first CI summary step did not produce a table (fixed by posting a PR comment); the plans sometimes included items the requirements did not support, so they were marked out of scope (see `docs/plans/US-04-rest-api-plan.md` section 4); I checked the free-play numbers against the rules by hand rather than trusting the output.
- Verification: domain tests run locally; full suite and CI checked on the PR.

## 8. Design documents

- Brief: `docs/candidate-brief-fruit-machine-take-home-1-2-.pdf`
- Business requirements: `docs/01-business-requirements.docx`
- Use cases: `docs/02-use-cases.docx`
- Conceptual design: `docs/03-conceptual-design.docx`
- Issue bodies, requirements (BR-xx, NFR-xx), decisions (D1–D11) and acceptance criteria (AC-01..36): `docs/gitHub_issue_bodies.md`
- Implementation plans and out-of-scope lists per issue: `docs/plans/US-01` to `US-06`
- Verification record (filled in Issue 7): `docs/verification.md`

## 9. Project layout

Packages under `com.thilaga.fruitmachine`:

- `api`: REST controllers, request/response records, error mapping
- `application`: `MachineService`, one transaction per use case
- `domain`: pure Java game rules (no Spring or JPA)
- `infrastructure`: JPA entities, repositories, Flyway migrations, spin generator
