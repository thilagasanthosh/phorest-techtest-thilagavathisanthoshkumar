# Implementation Plan: Issue 4, REST API

Sources:
- `docs/gitHub_issue_bodies.md` (Issue 4, lines 265-355): task list and Done-when.
- `docs/01-business-requirements.docx`: BR-01, BR-02, BR-08, BR-09, BR-10; AC-01 to AC-36; NFR-02, NFR-07, NFR-12, NFR-13; assumptions A3, A4, A6, A7, A9.
- `docs/02-use-cases.docx`: UC-01, UC-02, UC-03, UC-06, UC-07, UC-08, UC-09, UC-10, UC-11, UC-12.
- `docs/03-conceptual-design.docx`: section 5 (REST API: 5.1 conventions, 5.2 endpoints, 5.3 examples, 5.4 error responses), section 6 (transactions and locking).

Scope rule applied in this version: an item is in scope only if a requirement, use case, assumption, design rule or CLAUDE.md rule supports it. Items without that support are marked **OUT OF SCOPE** in section 4 and excluded from tasks.

Where the design and the issue body differ, the design governs the API contract. No `docs/specs/` feature spec exists; the issue body, requirements and design are the spec.

## 1. Prerequisites

- Issue 3 merged to `main` (PR #11).
- `pom.xml` has `spring-boot-starter-webmvc`, `spring-boot-starter-validation`, `spring-boot-starter-webmvc-test`. Springdoc is absent; Task 8 adds it.
- Domain already provides: `MachineConfig` validation (AC-04 via `MinimumReservePolicy`), `Machine.reservePence()`, `MachineStatus`, `MachineState`, `PlayResult`, `MachineNotFoundException`, `MachineBusyException`, `NoFreePlayAvailableException`, `MachineNeedsRefillException`, `MachineConfig.DEFAULT_*`.

## 2. Branch

`feature/4-rest-api` (checked out). PR body: `Closes #4`.

## 3. Gaps against the requirements and design

1. **Refusal body incomplete.** UC-10 and design 5.4 require `floatPence`, `reservePence` and `machineStatus` on the 409 needs-refill response. `MachineNeedsRefillException` carries none. (Task 0)
2. **Play response needs `playId` and `paidWithFreePlay`.** Design 5.3. `PlayResult` has neither. The play row already has an id (ERD-01 `play.id`, Issue 3). (Task 0)
3. **NFR-02 limits not enforced.** At most 10,000 slots and 1,000 colours. `MachineConfig` checks neither. (Task 1)
4. **Reserve is not an input.** A3. The request has no reserve field.
5. **Status is derived.** A6, BR-09. Computed from the float, never stored. NONE is always ACTIVE.
6. **Acceptance criteria missing from the issue body.** AC-04, AC-05, AC-17, AC-18, AC-27 are API-reachable per the requirements. Added to Tasks 3, 5, 7.
7. **Refused play side effects.** UC-10: no stake, no credit, no spin, no history record. Tests assert all four.
8. **Error codes and body shape.** Design 5.4 gives problem types, HTTP codes and the `machineStatus` field.
9. **Deadlock mapping.** Design 6.2. Owned by the repository translation. (Task 6)

## 4. Decisions and scope status

| ID | Decision | Status | Basis |
|---|---|---|---|
| D-api-1 | RFC 7807 `ProblemDetail`, `application/problem+json`, no new dependency | In scope | Design 5.1 (RFC 7807) |
| D-api-2 | Problem `type` uses the kebab names of design 5.4 as bare strings (`validation-error`, `machine-not-found`, `machine-needs-refill`, `no-free-plays`, `machine-busy`, `internal-error`). The UPPER_SNAKE codes in the design's AC table are labels of these types. | In scope | Design 5.4 |
| D-api-2a | `/problems/` base path for `type` URIs | **OUT OF SCOPE**: no rule requires it | none |
| D-api-3 | 400 body: `errors: [{field, message}]`. `field` is the JSON name where one exists; `null` for domain faults with no single field. | In scope (the `null` rule is an assumption) | Design 5.1 ("field errors where relevant"); UC-07 (list each fault) |
| D-api-4 | `503` with `Retry-After: 1` | In scope | Design 5.4 |
| D-api-5 | Bean Validation for shape and type; business rules stay in the domain | In scope | Design 5.4 (validation-error raised by MachineConfig); NFR-13 |
| D-api-6 | Skip API-05 (play history) and API-06 (list machines) | **OUT OF SCOPE**: optional in design 5.2, no use case (RN-07) | Design 5.2 |
| D-api-7 | `@SpringBootTest` + `MockMvc` + Testcontainers MySQL; test-only `@Primary` `FixedSpinGenerator` | In scope | CLAUDE.md (real MySQL, test-only forced spins); design 8 |
| D-api-8 | Springdoc OpenAPI / Swagger UI | In scope (Swagger required; the library choice is an assumption) | Design 5.1; Issue 4 Done-when |
| D-api-9 | Spring's own 404 (unknown route), 405, 415, and malformed UUID path variables mapped to their own statuses rather than 500 | **OUT OF SCOPE**: no requirement or design rule covers them. See the consequence in section 9. | none |
| D-api-10 | `Idempotency-Key` header | **OUT OF SCOPE**: design 5.1 says "later" | Design 5.1 |
| D-api-11 | `PlayResult` gains `playId` and `paidWithFreePlay`; `MachineService.play` generates `playId` | In scope | Design 5.3 (playId, paidWithFreePlay); design ERD play.id; edits Issue 3 domain code |
| D-api-12 | Malformed path id (not a UUID) returns 400 `validation-error` | **OUT OF SCOPE**: design does not state it. See the consequence in section 9. | none |

## 5. Response and request shapes (design 5.3)

Machine response (configure 201, read 200, refill 200):

```
{ id, slotCount, colours, k, playCostPence, floatPolicy, floatPence, reservePence,
  status, freePlays, totalPlays, totalPaidPence }
```

Play response (200):

```
{ playId, slots, prize, nominalPayout, paidOut, freePlaysCredited, freePlayBalance,
  floatAfter, paidWithFreePlay, status }
```

`slots` is the colour name per slot (mapped from colour ids using the machine's colours). `paidOut` = `PlayResult.paidPence`, `nominalPayout` = `nominalPence`, `freePlaysCredited` = `freePlaysEarned`, `freePlayBalance` = `freePlayBalanceAfter`, `floatAfter` = `floatAfterPence`.

Requests:

- Configure: `{ slotCount?, colours?, k?, playCostPence, startingFloatPence, floatPolicy? }`. Omitted optionals take `MachineConfig.DEFAULT_*` (BR-01).
- Play: `{ useFreePlay: boolean }`.
- Refill: `{ amountPence: long }`.

Error bodies (design 5.4):

| Situation | HTTP | type | Extra properties |
|---|---|---|---|
| Invalid or missing field, k > slots, duplicate colours, refill <= 0, malformed JSON, starting float below reserve | 400 | validation-error | `errors` |
| Unknown machine id | 404 | machine-not-found | |
| Float below reserve | 409 | machine-needs-refill | `floatPence`, `reservePence`, `machineStatus` |
| Free play requested, balance 0 | 409 | no-free-plays | |
| Lock not obtained in about 2 s, or deadlock | 503 + `Retry-After: 1` | machine-busy | |
| Anything else | 500 | internal-error | no stack trace |

## 6. Ordered tasks

TDD for every task: failing test, minimal code, `./mvnw fmt:format`, `./mvnw verify`, commit.

Integration tests end in `*Test` (Surefire only).

### Task 0: Domain data for refusals and play response

- **Satisfies:** UC-10 step 2, BR-07, BR-09, design 5.3 and 5.4, NFR-12.
- **Files:**
  - `domain/MachineNeedsRefillException.java`: add `floatPence`, `reservePence`.
  - `domain/Machine.java`: throw with `floatPence()` and `reservePence()`.
  - `domain/PlayResult.java`: add `playId` and `paidWithFreePlay` (D-api-11); update equals/hashCode/toString.
  - `application/MachineService.java`: generate `playId` (UUID) in `play`; pass it to the result and to the play record.
- **Test first:**
  - `MachineTest`: play on float 499, cost 100 throws with `floatPence() == 499`, `reservePence() == 500`; state unchanged.
  - `PlayResultTest`: equality includes `playId` and `paidWithFreePlay`.
  - `MachineServiceTest`: a play returns a non-null `playId`; `paidWithFreePlay` matches the request.
- **Verify:** `./mvnw test -Dtest=MachineTest,PlayResultTest,MachineServiceTest`
- **Commit:** `Carry refusal data and play id on domain results`

### Task 1: NFR-02 limits in MachineConfig

- **Satisfies:** NFR-02, NFR-07, NFR-13; AC-05 boundary.
- **Files:** `domain/MachineConfig.java`: two new checks, slots <= 10,000 and colours <= 1,000, added to the existing `errors` list.
- **Test first:** `MachineConfigTest`: 10,000 slots and 1,000 colours accepted; 10,001 and 1,001 refused with an error each; AC-03 checks still pass.
- **Verify:** `./mvnw test -Dtest=MachineConfigTest`
- **Commit:** `Enforce NFR-02 slot and colour limits in MachineConfig`

### Task 2: Problem-details error mapping

- **Satisfies:** NFR-12, NFR-13, UC-07, UC-09, UC-10, UC-11, UC-12, design 5.4, D-api-1 to D-api-5.
- **Files:** `api/ApiExceptionHandler.java` (`@RestControllerAdvice`):
  - `InvalidMachineConfigException`, `MethodArgumentNotValidException`, `HttpMessageNotReadableException` → 400 `validation-error`, `errors` as `{field, message}`.
  - `MachineNotFoundException` → 404 `machine-not-found`.
  - `MachineNeedsRefillException` → 409 `machine-needs-refill`, `floatPence`, `reservePence`, `machineStatus: NEEDS_REFILL`.
  - `NoFreePlayAvailableException` → 409 `no-free-plays`.
  - `MachineBusyException` → 503 `machine-busy`, `Retry-After: 1`.
  - Any other `Exception` → 500 `internal-error`, generic detail, no stack trace.
- **Test first:** `ApiExceptionHandlerTest` (standalone `MockMvc`, stub controller throws each): status, `Content-Type: application/problem+json`, `type`, `errors` shape, 409 properties, `Retry-After`, no stack-trace text in 500.
- **Verify:** `./mvnw test -Dtest=ApiExceptionHandlerTest`
- **Commit:** `Map domain and validation errors to design problem types`

### Task 3: POST /api/v1/machines (configure, API-01)

- **Satisfies:** BR-01, UC-01, AC-01, AC-02, AC-03, AC-04, AC-05, AC-31 (configure side).
- **Files:**
  - `api/MachineController.java`: POST, 201, `Location: /api/v1/machines/{id}`, body = machine response.
  - `api/CreateMachineRequest.java`: optional `slotCount`, `colours`, `k`, `floatPolicy`; `@NotNull @Positive playCostPence`; `@NotNull startingFloatPence`. No reserve field.
  - `api/MachineResponse.java`: shape of section 5.
  - Mapping fills nulls from `MachineConfig.DEFAULT_*`.
- **Test first:** `MachineCreateApiTest` (`@SpringBootTest`, `MockMvc`, MySQL):
  - AC-01: 201, `Location` matches the returned id, exact settings, `status: ACTIVE`, `reservePence: 500`.
  - AC-02: only cost and float supplied → defaults.
  - AC-03: k = 0; k = 5 with 4 slots; duplicate colours; cost 0; negative float → 400 `validation-error`, every fault listed; no row created.
  - AC-04: MIN_RESERVE, float 499 → 400; float 500 → 201.
  - AC-05: 10 slots, 300 colours, k = 3, cost 50, float 1,000 → 201 with those settings.
- **Verify:** `./mvnw test -Dtest=MachineCreateApiTest`
- **Commit:** `Add POST /api/v1/machines`

### Task 4: GET /api/v1/machines/{id} (read state, API-03)

- **Satisfies:** BR-09, UC-06, AC-28, AC-29, AC-18 (read side).
- **Files:** `api/MachineController.java` (GET). Uses `MachineResponse`. Reserve from `Machine.reservePence()`; status from `MachineStatus` (derived).
- **Test first:** `MachineReadApiTest`:
  - AC-28: all fields returned, status ACTIVE.
  - AC-29: unknown UUID → 404 `machine-not-found`; nothing changes.
  - NONE machine at float 0 → status ACTIVE (BR-09).
- **Verify:** `./mvnw test -Dtest=MachineReadApiTest`
- **Commit:** `Add GET /api/v1/machines/{id}`

### Task 5: POST /api/v1/machines/{id}/refills (API-04)

- **Satisfies:** BR-08, UC-08, AC-18 (refill step), AC-19, AC-20, AC-21, AC-30 (refill record).
- **Files:** `api/MachineController.java` (POST, 200, body = `MachineResponse` after refill); `api/RefillRequest.java` (`@NotNull @Positive amountPence`).
- **Test first:** `MachineRefillApiTest`:
  - AC-19: float 0 + 5,000 → float 5,000, status ACTIVE, one refill record.
  - AC-20: 0 and -1 → 400 `validation-error`; state and record count unchanged.
  - AC-21: unknown id → 404; nothing changes.
- **Verify:** `./mvnw test -Dtest=MachineRefillApiTest`
- **Commit:** `Add POST /api/v1/machines/{id}/refills`

### Task 6: Lock timeout and deadlock mapping in persistence

- **Satisfies:** design 6.2, UC-12, AC-32 (busy path only; the concurrency test is Issue 5).
- **Files:** `infrastructure/persistence/JpaMachineRepository.java`: translate the lock-timeout and deadlock exceptions raised by `findForUpdate` into `MachineBusyException`.
- **Status:** already implemented in Issue 3 (`JpaMachineRepository.findForUpdate` translates MySQL 1205/1213). No change in Issue 4.
- **Covered by:** existing `JpaMachineRepositoryTest.deadlockIsReportedAsBusyForExactlyOneOfTwoOppositeLockers` and `lockTimeoutIsReportedAsBusyWithinTwoSeconds`.
- **Verify:** `./mvnw test -Dtest=JpaMachineRepositoryTest`
- **Commit:** `Map lock timeout and deadlock to machine busy`

### Task 7: POST /api/v1/machines/{id}/plays (API-02)

- **Satisfies:** BR-02, BR-07, UC-02, UC-03, UC-10, UC-11, AC-15, AC-16, AC-17, AC-18 (jackpot step), AC-25, AC-26, AC-27, AC-29, AC-36, NFR-05.
- **Files:**
  - `api/MachineController.java`: POST plays, 200.
  - `api/PlayRequest.java`: `@NotNull Boolean useFreePlay`.
  - `api/PlayResponse.java`: shape of section 5.
  - Mapping of colour ids to names and `PlayResult` fields to response fields.
- **Test first:** `MachineInteractionApiTest`, with test-only `FixedSpinGenerator`:
  - AC-15: small prize forced; response has every field of section 5, `paidWithFreePlay: false`.
  - AC-16 over HTTP: a drained machine (float 0, after a jackpot) → 409 `machine-needs-refill` with `floatPence: 0`, `reservePence: 500`, `machineStatus: NEEDS_REFILL`. Float, balance, totals, history count and spin count unchanged. The 499p boundary cannot be reached over HTTP (AC-04 refuses a 499p start); it is tested in `MachineTest.rejectsPlayBelowReserve_noStateChange`.
  - AC-17: float 500, small prize forced → 200; `floatAfter: 100`, `paidOut: 500`, `freePlaysCredited: 0`.
  - AC-18 (jackpot): jackpot forced on MIN_RESERVE → float 0; read gives NEEDS_REFILL; refill 1,000 → read gives ACTIVE.
  - AC-25: NONE, 1 free play, float 0, small prize forced → `paidOut: 0`, `freePlaysCredited: 5`, `freePlayBalance: 5`, `floatAfter: 0`, `paidWithFreePlay: true`.
  - AC-26: balance 0, `useFreePlay: true` → 409 `no-free-plays`; spin not called.
  - AC-27 is **not tested over HTTP**: MIN_RESERVE never earns free-play credits (A10), so a held credit cannot be created through the API. Tested in `MachineTest.heldFreePlaySurvivesARefusal` (domain).
  - AC-36: `{"useFreePlay":"yes"}`, malformed JSON, missing field → 400 `validation-error`; state unchanged.
  - AC-29: unknown id → 404; nothing changes.
- **Verify:** `./mvnw test -Dtest=MachineInteractionApiTest`
- **Commit:** `Add POST /api/v1/machines/{id}/plays`

### Task 8: OpenAPI / Swagger UI

- **Satisfies:** design 5.1 ("Swagger UI / OpenAPI documents and exercises the endpoints"), Done-when.
- **Files:** `pom.xml` (springdoc starter, D-api-8); `@Schema(example = ...)` on request and response fields, using the design 5.3 examples.
- **Test first:** `OpenApiApiTest`: `GET /v3/api-docs` lists the four paths (`/api/v1/machines`, `/{id}`, `/{id}/plays`, `/{id}/refills`); `GET /swagger-ui.html` returns 200 or a redirect.
- **Verify:** `./mvnw test -Dtest=OpenApiApiTest`
- **Commit:** `Expose OpenAPI docs and Swagger UI`

### Task 9: Postman collection for API testing

- **Satisfies:** design 8.3 (collections exercise API-01 to API-04), Done-when ("works from Postman"), section 7 acceptance coverage.
- **Files:** `postman/fruitmachine-api.postman_collection.json` (Postman v2.1, 33 requests in 4 folders: configure, read, play, refill). Generated by `postman/generate-collection.js` (committed); regenerate with `node postman/generate-collection.js postman/fruitmachine-api.postman_collection.json`.
- **Coverage:** AC-01 to AC-05, AC-07, AC-14 to AC-21, AC-26, AC-28, AC-29, AC-36, NFR-02, NFR-12 (problem type and content type checked on every refusal). Jackpot cases use a one-slot, one-colour machine (certain outcome).
- **Not covered over HTTP:** AC-16 at 499p and AC-27 (unreachable; both tested in `MachineTest`). AC-25 is tested over HTTP through the NONE-policy shortfall arithmetic (`MachineInteractionApiTest.freePlayCreditIsEarnedAndSpentOnNoneMachine`). 503 busy and AC-32 need concurrent load (Issue 5). Stated in the collection description.
- **Verify:** start the service (`./mvnw spring-boot:run`), then `newman run postman/fruitmachine-api.postman_collection.json --env-var baseUrl=http://localhost:8080`. Expected: all tests pass. Install Newman first (not present on this machine).
- **Commit:** `Add Postman collection for Issue 4 API`

### Done-when check (manual, not a task)

Run configure → play → refused → refill → play with `./mvnw spring-boot:run` (Testcontainers dev instance) using Swagger UI or curl. Record in the PR body.

## 7. Requirement and design coverage

| Source | Where covered |
|---|---|
| BR-01 configure, defaults | Task 3 |
| BR-02 paid play result | Task 7 |
| BR-07 reserve refusal | Tasks 0, 2, 7 |
| BR-08 refill | Task 5 |
| BR-09 state, derived status | Tasks 4, 7 |
| BR-06 free plays | Task 7 (AC-25, AC-26 over HTTP; AC-27 in MachineTest) |
| NFR-02 limits | Task 1 |
| NFR-05 all-or-nothing (API side) | Task 7 |
| NFR-07 invalid state | Task 1 |
| NFR-12 status categories | Task 2 |
| NFR-13 invalid input changes nothing | Tasks 3, 5, 7 |
| Design 5.2 endpoints API-01 to API-04 | Tasks 3, 4, 5, 7 |
| Design 5.3 examples | Section 5, Tasks 3, 4, 5, 7 |
| Design 5.4 error table | Section 5, Task 2 |
| Design 6.2 deadlock and timeout | Task 6 |
| A3, A4, A6, A9 | Tasks 3, 4, 7 |

## 8. Out of scope for this issue

No assumption, requirement, use case, design rule or CLAUDE.md rule supports these; they are excluded from every task.

- API-05 play history and API-06 list machines (D-api-6).
- `Idempotency-Key` header (D-api-10).
- `/problems/` base path for problem types (D-api-2a).
- Spring's own 404/405/415 and malformed-UUID 400 handling (D-api-9, D-api-12). See section 9 for the consequence.
- AC-22 to AC-24, AC-34, AC-35 (domain, Issue 2).
- AC-30 and AC-31 full checks (Issue 3).
- AC-32 and AC-33 concurrency tests (Issue 5).
- System tests over HTTP and `verification.md` (Issue 7).

## 9. Test strategy

- **Unit (no Spring):** `MachineTest`, `PlayResultTest`, `MachineConfigTest`, `ApiExceptionHandlerTest`.
- **Integration (Spring + Testcontainers MySQL, MockMvc):** Tasks 3, 4, 5, 6, 7, 8. No H2.
- **Forced spins:** test-only `@Primary` `FixedSpinGenerator`, excluded from production profile.

## 10. Definition of Done

- [ ] Tasks 0 to 8 committed, each test written first.
- [ ] `./mvnw verify` green locally and in CI.
- [ ] `./mvnw fmt:format` clean.
- [ ] Every API-reachable AC in section 7 has a passing test: AC-01 to AC-05, AC-15 to AC-21, AC-25 to AC-29, AC-36.
- [ ] Every response matches section 5 field names exactly.
- [ ] Every error is RFC 7807 with a design 5.4 type; 409 needs-refill carries `floatPence`, `reservePence`, `machineStatus`; 503 carries `Retry-After: 1`.
- [ ] 500 has no stack trace.
- [ ] Every refused request leaves float, free-play balance, history and spin count unchanged.
- [ ] Reserve is not accepted as input; status is not stored.
- [ ] `domain` has no Spring or JPA imports (review check).
- [ ] Swagger UI shows the four endpoints.
- [ ] PR body: `Closes #4`, and the out-of-scope list from section 8.

## 11. Estimate, risks, decisions to confirm

- **Task count:** 10 (Tasks 0 to 9) plus the manual check.
- **Risks:**
  - Task 0 and Task 1 edit Issue 3 and Issue 2 domain code (`PlayResult`, `MachineConfig`). Keep diffs to the listed fields.
  - Springdoc on Spring Boot 4.1 / Jackson 3: version mismatch is the likeliest build break.
  - `ProblemDetail` extra properties must be verified in Task 2, not assumed.
  - Jackson parse-error wording varies; tests assert that an `errors` entry exists, not its text.
  - Design 5.4 and the design's AC table name error codes differently. Section 4 uses the kebab types of 5.4 (D-api-2).
- **Consequence of the out-of-scope items (must decide):** with D-api-9 and D-api-12 out of scope, Spring's unknown-route 404, unsupported-media-type 415, method-not-allowed 405 and a malformed UUID in the path all fall into the design's catch-all and return **500 `internal-error`**. Options:
  1. Accept 500 for these (strictly in scope with the design; poor for clients).
  2. Add the mappings as a small follow-up rule, after the design is updated to cover them.
- **Decisions to confirm before Task 0:**
  1. D-api-2a: confirm bare type strings (no `/problems/` base path).
  2. Consequence above: option 1 or option 2.
  3. D-api-11: `playId` and `paidWithFreePlay` added to `PlayResult` (touches Issue 3 code).
  4. Section 8: confirm the out-of-scope list.
- **Open question:** `docs/specs/US-04-rest-api.md`: CLAUDE.md lists a feature spec before the plan; US-01 to US-03 had none.
