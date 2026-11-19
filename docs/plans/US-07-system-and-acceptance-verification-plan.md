# Implementation Plan: Issue 7, System and acceptance verification

Sources:
- `docs/gitHub_issue_bodies.md` (Issue 7, "System and acceptance verification"): goal, scope, tasks, scenario matrix, exit criteria, Done-when.
- `docs/02-use-cases.docx`: SC-01 to SC-13 (scenario definitions).
- `docs/01-business-requirements.docx`: AC-01 to AC-36 (titles in the issue bodies, Issues 2 to 4).
- `docs/verification.md`: result table already created (uncommitted on this branch).
- `docs/plans/US-04-rest-api-plan.md`: API shapes and problem types the tests assert against.

No `docs/specs/` feature spec exists for any issue so far. Following the earlier plans, the issue body and requirements are the spec.

## 1. Prerequisites

- Issues 1 to 6 merged to `main`. Confirmed: PRs #11, #12, #13 are merged (Issues 3, 4, 5). Issue 6 (`docker compose`, README) is in `main` history (`efd416e`, `fc3c67a`). Re-check CI green on `main` before starting.
- `docker compose up -d --build` works on a clean checkout (entry criterion).
- Docker running locally for Testcontainers.
- Existing test base: `TestcontainersConfiguration`, `TestTables`, `TestFruitmachineApplication` (src/test). Reuse them; do not add a second MySQL setup.
- Domain already has `FixedSpinGenerator` (src/main/.../domain) and `RandomSpinGenerator` (src/main/.../infrastructure). Issue 5 added concurrency and end-to-end tests. Those are not repeated here; Issue 7 adds the scenario-level evidence.

## 2. Branch

`feature/7-verification` (checked out). PR body: `Closes #7`.

## 3. Gaps against the issue and requirements

1. **Scripted spins already exist.** `ScriptedSpins` and `ScriptedSpinsConfig` (`@TestConfiguration`, `@Primary`) are in `src/test/.../api`. Reuse them; add only a guard test that production wiring uses `RandomSpinGenerator`. (Task 1)
2. **Traceability matrix does not exist.** The issue asks to "update the master traceability matrix". No file in `docs/` or `README.md` holds one (grep for "traceability" finds only the issue body). Plan: create it as a section of `docs/verification.md` (requirement, test, result), not a new document. Confirm this placement before Task 9.
3. **AC-34 (fair spins) and AC-35 (1,000,000 slots < 100 ms) are not system-level.** NFR-15 and NFR-01 need statistical and timing checks. Plan: check whether Issue 2 tests already cover them. If yes, cite them in `verification.md` and do not duplicate. If not, add the domain-level check. (Task 7)
3a. **AC-33 needs a rule for busy results.** With a 2 s lock timeout and 200 parallel plays on one machine, some plays return 503 `machine-busy`. Acceptance wording "money stays consistent" is measured as: successes + busy = 200, and `float_end = float_start + stakes − paid_out` holds exactly, with no negative float and no stake recorded for a busy play. This is the assertion; say so in `verification.md`.
4. **Postman collection: contents not checked.** `postman/` exists. Plan starts by listing its contents; if it has a collection already, extend it and do not create a second one.
5. **Newman is not in the repo.** Run it with `npx newman` (Node required) or document the install. The issue says "runnable with Newman"; the exact command goes in the README section for verification. Decide Node version when writing the README line.
6. **Scenario mapping to AC is not in the source.** The issue gives a matrix by SC, not by AC. Plan: each system test name carries its SC and AC ids (e.g. `sc04_jackpot_forced_spin_ac07`), so `verification.md` can be filled from test names.

## 3b. Test tiers and IT tests

Repo convention: existing tests use Surefire's `*Test` naming. `pom.xml` has no Failsafe config (checked). Decision: system tests added in this issue are `*IT` classes run by Failsafe, so the API system verification is separate from unit and MockMvc tests. Existing `*Test` classes are not renamed.

| Tier | Spring context | Database | HTTP | Package | Existing examples |
|---|---|---|---|---|---|
| Unit | none (plain Java) | none | none | `domain` | `MachineTest`, `PrizeEvaluatorTest`, `MachineConfigTest` |
| IT (integration) | Spring Boot, Testcontainers MySQL | real MySQL | MockMvc (in-process) | `infrastructure`, `application`, `api` | `JpaMachineRepositoryTest`, `SchemaConstraintsTest`, `MachineHistoryTest`, `MachineServiceTest`, `MachineCreateApiTest`, `MachineInteractionApiTest`, `MachineRefillApiTest`, `MachineReadApiTest`, `OpenApiApiTest` |
| System (Issue 7) | Spring Boot on `RANDOM_PORT`, Testcontainers MySQL | real MySQL | real HTTP | `system` (new), `*IT` via Failsafe | none yet |
| Acceptance (Issue 7) | full stack via `docker compose up` | real MySQL | real HTTP | Postman collection in `postman/`, run with Newman | none yet |

Rules for this issue:
- IT tests already cover persistence, schema constraints, service behaviour and API shape. Issue 7 does not repeat them.
- Task 0 audits IT coverage against AC-01..36. New IT tests are added only for ACs with no IT. They use MockMvc and MySQL, not the random-port system base.
- System `*IT` tests are added for scenario outcomes that need the real HTTP server (SC-01..13). They verify API behaviour against the spec. They do not replace acceptance.
- Acceptance is the Postman/Newman run against `docker compose up` (Task 8), recorded in `verification.md`. This repo has no UI, so no Cucumber or Playwright layer.
- All tiers use real MySQL. No H2 (CLAUDE.md).

## 4. Decisions and scope status

| ID | Decision | Status | Basis |
|---|---|---|---|
| V-1 | System tests: `@SpringBootTest(webEnvironment = RANDOM_PORT)`, real MySQL via Testcontainers, HTTP client against the random port | In scope | Issue 7 scope; CLAUDE.md (real MySQL) |
| V-2 | Scripted `SpinGenerator` is a `@TestConfiguration` with `@Primary`, imported only by system tests | In scope | Issue 7 scope; CLAUDE.md (test-only forced spins) |
| V-3 | Use the HTTP client the Boot 4.1 test starter provides (`RestTestClient` or `TestRestTemplate`); no new dependency | In scope | CLAUDE.md "never add a dependency for a few lines" |
| V-4 | Each test asserts the exact response body and the stored state (float, free plays, history) for exact scenarios | In scope | Issue 7 matrix ("Exact") |
| V-5 | UAT response checks are rule checks (payout = prize, float arithmetic), not fixed numbers, because spins are random | In scope | Issue 7 UAT scope |
| V-6 | Postman collection stored in `postman/`, with an environment file for `baseUrl` | In scope | Issue 7 task 3 |
| V-7 | Newman run recorded to `docs/verification/evidence/` | In scope | `verification.md` plan (evidence folder) |
| V-8 | Stress-test SC-13 with 200 parallel plays in the system test, not in UAT | In scope | Issue 7 matrix |
| V-9 | k6 or Postman Runner for SC-13 UAT | **OUT OF SCOPE**: the matrix says "Optional" | Issue 7 matrix |
| V-10 | New business features or API endpoints | **OUT OF SCOPE**: verification only | Issue 7 goal |
| V-12 | System tests are `*IT` run by Failsafe; acceptance is Postman/Newman. No Cucumber or Playwright (no UI; not required by this repo's CLAUDE.md) | In scope | User decision, Issue 7 planning |
| V-11 | Fixing product defects found here | **OUT OF SCOPE** unless a blocking defect is found, then a separate fix on this branch with its own test. Record the defect first. | Issue 7 exit criteria |

## 5. Test and acceptance mapping

Each row is one system test class or method. Names carry their SC and AC ids.

| Scenario | Forced spin / setup | Asserts (exact) | ACs covered |
|---|---|---|---|
| SC-01 | 4 slots, k=2, cost 100p, float 1000p, MIN_RESERVE or omitted | Create 201, defaults, status ACTIVE, reserve 500p | AC-01, AC-02, AC-18 |
| SC-02 | 10 slots, 300 colours, k=3, cost 50p, float 1000p | Create 201, large config accepted | AC-05 |
| SC-03 | Spins: black, white, black, white | No win: float +stake, no payout, history row | AC-10 |
| SC-04 | green ×4 | Jackpot: payout, float set to 0, no free play created | AC-07, AC-24 |
| SC-05 | black, white, green, yellow; float 1001p | Full house: 550p paid, 551p remain | AC-08, AC-12 |
| SC-06 | black, black, white, green | Small prize | AC-09, AC-11 |
| SC-07 | float 300p, NONE, small prize | Shortfall becomes free plays | AC-22 |
| SC-08 | float 250p, NONE, small prize | Free plays rounded up | AC-23 |
| SC-09 | NONE, float 0p, 1 free play, small prize | Credit used, no stake; with balance 0 refused | AC-25, AC-26 |
| SC-10 | MIN_RESERVE, cost 100p, float 499p | 409 `machine-needs-refill`, body has floatPence/reservePence/machineStatus; at 500p accepted | AC-16, AC-17 |
| SC-11 | float 0p, NEEDS_REFILL | Refill 5000p, state ACTIVE, history row | AC-19, AC-30 |
| SC-12 | k=5 with 4 slots; unknown machine id | 400 `validation-error` for config; 404 `machine-not-found` for play | AC-03, AC-06, AC-14?, AC-29, AC-36 |
| SC-13 | MIN_RESERVE, float 500p, cost 100p | Two plays concurrently: both accepted or one busy, never both over-spend; then 200 parallel (see 3a) | AC-31, AC-32, AC-33 |
| Not a scenario | Refill amount ≤ 0; refill unknown machine; floats; slot/colour limits | 400 / 404 | AC-04, AC-13, AC-15, AC-20, AC-21, AC-27, AC-28 |

AC-14 (one-slot machine) and AC-34/AC-35 are covered by Task 7 and Task 1 below. Any AC with no assertion after Task 6 is listed in `verification.md` as an open gap, not silently left out.

## 6. Ordered task list (TDD)

For each task: failing test → minimal code (or config) → `./mvnw fmt:format` → commit with path-limited `git add` (per project commit notes).

**Task 0, IT coverage audit (section 3b)**
- List each AC-01..36 with the IT test that covers it, or "none".
- Test first, only for ACs marked "none": MockMvc + MySQL IT test, then minimal code if it fails.
- Verify: `./mvnw test` (all IT tests green, existing names unchanged).

**Task 1, scripted spins guard (V-2)**
- Test first: `ProductionWiringTest` asserts the default context has exactly one `SpinGenerator` bean and it is `RandomSpinGenerator` (AC-34 guard).
- Code: none new. Reuse `ScriptedSpins` and `ScriptedSpinsConfig` (`src/test/.../api`). Confirm nothing in `src/main` references them.
- Verify: `./mvnw test -Dtest=ProductionWiringTest`.

**Task 2, system test base (V-1, V-3)**
- Test first: `SystemTestSmokeTest` creates a machine over HTTP on the random port and reads it back.
- Add `maven-failsafe-plugin` to `pom.xml` (version managed by the Spring Boot parent; confirm it is present) with `integration-test` and `verify` goals, including `**/*IT.java`.
- Code: `SystemTestBase` in `src/test/.../system`: `@SpringBootTest(webEnvironment = RANDOM_PORT)`, `@Import({TestcontainersConfiguration.class, ScriptedSpinsConfig.class})`, an HTTP client bean (`RestTestClient` or `TestRestTemplate`), and state reset. Do not extend `AbstractApiTest` (MockMvc, no real HTTP).
- Verify: `./mvnw test -Dtest=SystemTestSmokeTest`.

**Task 3, SC-01 and SC-02 configure (AC-01, AC-02, AC-05, AC-18)**
- Test first: one test per SC, exact body assertions as in section 5.
- Code: none expected; fix only if a test fails (report the defect first).

**Task 4, SC-03 to SC-06 prizes (AC-07 to AC-12)**
- Test first: forced spins for each scenario; assert payout, float, history.
- Verify: `./mvnw test -Dtest='Sc0*PrizeSystemTest'`.

**Task 5, SC-07 to SC-09 free plays (AC-22 to AC-26)**
- Test first: NONE policy scenarios; assert free-play balance and no stake on credit use.

**Task 6, SC-10 to SC-12 refusal, refill, invalid input (AC-03, AC-06, AC-16, AC-17, AC-19 to AC-21, AC-29, AC-36)**
- Test first: refusal leaves no stake, no credit, no spin, no history (UC-10). Assert the 409 body.

**Task 7, SC-13 and remaining ACs (AC-31 to AC-35)**
- Test first: two simultaneous plays via `CountDownLatch`; then 200 parallel plays via `ExecutorService`; assert the rule in 3a.
- AC-34 and AC-35: check Issue 2 tests first (section 3, item 3). Add a test only if missing.
- Verify: `./mvnw test -Dtest='Sc13*'` (system), plus the domain check if added.

**Task 8, Postman collection and Newman (V-6, V-7)**
- Check `postman/` contents first (section 3, item 4).
- Collection covers: configure, play, refused, refill, invalid input, unknown machine. Assertions check response rules (payout = prize, float arithmetic) so random spins pass.
- Use a one-slot machine for the certain jackpot (SC-04 UAT, SC-10/11 drain).
- Verify: `docker compose up -d --build`, then `npx newman run postman/<collection>.json -e postman/<env>.json -r cli,json --reporter-json-export docs/verification/evidence/uat-run.json`.

**Task 9, verification record and traceability (Issue exit criteria)**
- Fill `docs/verification.md`: every AC and SC row gets method, result, date, evidence link.
- Add traceability section (section 3, item 2): requirement → test name → result.
- Re-read RN-01 to RN-11; list open items in `verification.md`.
- Copy surefire reports for the run to `docs/verification/evidence/`.

## 7. Test strategy

- System tests (automated, black box): real HTTP, real MySQL, scripted spins. Run by `./mvnw verify`, so CI runs them.
- UAT: Postman/Newman against `docker compose up`. Random outcomes are checked by rule, not fixed numbers.
- Unit or domain checks only for AC-34/AC-35 if not already covered (no Spring or JPA).
- No H2 or in-memory database (CLAUDE.md).

## 8. Definition of Done

- [ ] Task 1 to 8 tests pass locally and in CI (`./mvnw verify` green on the branch).
- [ ] `docker compose up` run completed; Newman run recorded in `docs/verification/evidence/`.
- [ ] Every AC-01..36 and SC-01..13 row in `docs/verification.md` has a result and date; any non-pass is explained.
- [ ] Traceability section present.
- [ ] RN-01..11 re-read and open items listed.
- [ ] Scripted spin config is test-only (Task 1 guard passes).
- [ ] All existing tests pass unchanged; Task 0 tests use `*Test` naming and real MySQL.
- [ ] `./mvnw verify` runs the system `*IT` classes; `target/failsafe-reports/` lists them (check before relying on it).
- [ ] No open blocking defect.
- [ ] Postman collection, environment, and `verification.md` committed; PR body `Closes #7`.

## 9. Risks and open questions

1. **AC-33 pass rule:** section 3a is my reading of "money stays consistent" with busy results. Confirm before Task 7.
2. **Traceability placement:** confirm a section inside `verification.md` (section 3, item 2) rather than a separate file.
3. **AC-14 and AC-34/35 coverage:** unclear from the matrix; resolved by checking Issue 2 tests in Task 7.
4. **Random spins in UAT:** a rule-checking UAT cannot prove exact prize outcomes; exact proof comes from the system tests.
5. **Missing spec stage:** CLAUDE.md requires `docs/specs/US-07-...md` before this plan. None exists for any issue, so this plan follows earlier plans. Say if you want the spec written first.
6. **Newman and Node version:** needed for Task 8; not yet in the repo.
7. **Failsafe version:** confirm the Spring Boot parent manages `maven-failsafe-plugin`; otherwise pin a version (check it is at least two weeks old).
8. **Unchecked:** `postman/` contents, whether Issue 2 tests cover AC-34/35, and CI status on `main` were not verified in this planning step.

## 10. Estimated size

9 tasks. Largest risk: Task 7 (concurrency, flaky timing). Expect at least 3 reruns to confirm stability.
