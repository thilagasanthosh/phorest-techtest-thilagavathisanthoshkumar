# phorest-techtest-thilagavathisanthoshkumar

Fruit Machine Service: a Spring Boot REST API for the take-home brief. It configures a machine (slots, colours, k, play cost, float, float policy), plays it, refills it, and reports its state. Machine state is stored in MySQL.

Brief parts covered: core spin and jackpot (Part 1), payouts and free-play shortfall (Part 2), variable slots, colours and k (Part 3), REST service (Part 4). Optional extras done: Docker Compose, CI, persistence, Swagger, metrics.

## 1. How to run

### Prerequisites

- JDK 21 (the bundled formatter fails on newer JDKs)
- Docker, running (tests and the dev launcher use MySQL through Testcontainers)
- Optional: Newman for the command-line Postman run (`npm i -g newman`)

### Java 21 setup (do this first)

The build needs JDK 21 exactly. The Maven enforcer stops the build on any other version, and the formatter crashes on JDK 27. Check before anything else:

```
java -version        # must show "21.x"
./mvnw -v            # "Java version: 21.x" on the last lines
```

If either shows another version, set `JAVA_HOME` to a JDK 21 install for this shell:

- **macOS:** `export JAVA_HOME=$(/usr/libexec/java_home -v 21)`
- **Linux:** `export JAVA_HOME=/usr/lib/jvm/<jdk-21-folder>`
- **Windows (PowerShell):** `$env:JAVA_HOME="C:\Program Files\Java\jdk-21"`

Make it permanent by adding the export line to `~/.zshrc` or `~/.bashrc` (macOS/Linux), or by setting `JAVA_HOME` under System environment variables (Windows). Restart the terminal afterwards.

**IntelliJ IDEA settings** (File → Settings on Windows/Linux, IntelliJ IDEA → Settings on macOS):

1. **Build, Execution, Deployment → Build Tools → Maven → Runner → JRE:** select a JDK 21 entry (not "Project JDK" if that is 27).
2. **Build, Execution, Deployment → Build Tools → Maven → Importing → JDK for importer:** select JDK 21.
3. **File → Project Structure → Project → SDK:** select JDK 21. **Language level:** 21.
4. **File → Project Structure → Modules → Language level:** 21.
5. Run configurations (the dev launcher, tests): **Edit Configuration → JRE:** JDK 21, or leave as "Project SDK" once step 3 is set.
6. If IntelliJ was open before changing these, reload the Maven project (Maven panel → reload icon).

Confirm with `./mvnw -B verify` from the IDE terminal: the output must show `BUILD SUCCESS`.

**Before you start the project, please confirm all of these:**

- [ ] `java -version` shows 21.x in the terminal you will build from
- [ ] `./mvnw -v` shows JDK 21
- [ ] IntelliJ Project SDK and Maven runner JDK are both 21
- [ ] `./mvnw verify` ends with `BUILD SUCCESS`

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
- **Authentication and RBAC** (JWT resource server, `OPERATOR` and `PLAYER` roles, per-role tests). Deliberately deferred: the brief has no auth requirement, and it adds an issuer and token handling. Keycloak only if time allows.
- **Idempotency-Key header** for plays. Design 5.1 says "later". Planned in the roadmap, so a retried request does not play twice.
- **Play history and list-machines endpoints** (API-05, API-06). Optional in design 5.2; no use case. Pagination on history is the next step if added.
- **Cloud deployment** (RDS MySQL, Secrets Manager or Parameter Store, Terraform or CDK, App Runner or ECS Fargate). Compose covers the one-command run (NFR-16); EKS is costly and not needed here.
- **Resilience4j timeouts and circuit breaking** around the database call. The 2 s pessimistic lock timeout is the only bound today.
- **Rate limiting** (Bucket4j or a gateway). The NFR-17 input limits cover payload size; there is no request-rate limit.
- **OpenAPI depth**: summaries and descriptions on records, documented problem-details responses (400/404/409/503), and examples for play and refill. The OpenAPI paths exist; the detail does not yet.
- **404/405/415 and malformed path ids mapped to their own statuses.** Currently they can fall into the catch-all 500. Open decision in `docs/plans/US-04-rest-api-plan.md` (section 9).
- **Property-based tests** (jqwik) and **load tests beyond 200 plays**. No acceptance criterion; would add dependencies.
- **Distributed tracing** (Micrometer Tracing / OpenTelemetry). The per-play log line covers NFR-14; ask before adding a dependency.

What I would do next, in order (full plan in `docs/roadmap.md`):

1. Fix the 404/405/415 mapping so they stop falling into the 500 catch-all.
2. Decide the free-play rule for MIN_RESERVE and the NONE loop (needs a spec decision).
3. Core hardening: OpenAPI depth, Actuator metrics, Idempotency-Key on `POST /plays`.
4. Authentication: JWT resource server, then RBAC with per-role tests; Keycloak only if time allows.
5. Rate limiting and Resilience4j around the DB call.
6. Deploy: secrets outside env vars, RDS MySQL 8 with a tested restore, IaC, and a small managed runtime.
7. Add the migrator DB user and cache the Maven distribution in CI.

## 7. Where AI was used

- Used for: writing the issue plans and specs in `docs/`, drafting the code per issue, writing and reviewing tests, and the Compose, CI and Grafana setup.
- Disagreed with or reworked: the first CI summary step did not produce a table (fixed by posting a PR comment); the plans sometimes included items the requirements did not support, so they were marked out of scope (see `docs/plans/US-04-rest-api-plan.md` section 4); I checked the free-play numbers against the rules by hand rather than trusting the output.
- Verification: domain tests run locally; full suite and CI checked on the PR.

### AI usage prompts

Prompts used with Claude Code, in workflow order. Each issue followed the same stages: GitHub issue → feature spec → implementation plan → implementation → tests → review → PR.

**1. Understand requirements**

> Read the brief and the business requirements, use cases and conceptual design. List the business rules (BR-xx), use cases (UC-xx) and decisions (D1–D11) with their source. Flag any conflict or ambiguity between the documents. Do not invent requirements.

**2. Derive design documents**

> Act as a Senior Java Solution Architect. Create a feature specification for Issue #N. Read `docs/gitHub_issue_bodies.md` (Issue #N), the three design documents and `docs/specs/`. Output `docs/specs/US-NN-<slug>.md` with: story information, requirement traceability (BR, UC, AC), functional specification, technical design boundaries, Given/When/Then acceptance scenarios, testing expectations, dependencies and out of scope, and Definition of Done. Constraints: one Spring Boot app, pure Java domain, no microservices, no separate frontend, one focused PR. Do not write implementation code yet.

**3. Plan the work**

> Act as a Senior Java Solution Architect and TDD Lead. Create an implementation plan for Issue #N from its spec and the existing `src/`. Output `docs/plans/US-NN-<slug>-plan.md` with prerequisites, branch name `feature/<n>-<slug>`, an ordered task list (AC covered, files, first failing test, minimal code, verify step), the TDD cycle, test strategy and Definition of Done. Constraints: money as integer pence, Testcontainers MySQL (no H2), decisions D1–D11 settled. Do not write source code yet. End with task count, risks and open questions.

**4. Build (TDD, one task at a time)**

> Implement task N of the plan. Write the failing test for its acceptance criterion first, then the minimal code, then run `./mvnw fmt:format`. Do not change code outside the task. Show the test result.

**5. Review and verify**

> Review the diff for correctness bugs, broken project rules and missing tests. Report only verified findings with file and line. Then check the acceptance criteria one by one against the running app and record the result in `docs/verification.md`.

**6. Open the PR**

> Commit only the paths for this issue. Open a PR from `feature/<n>-<slug>` to `main` with the body `Closes #<n>`.

## 8. Design documents

- Brief: take-home brief
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
