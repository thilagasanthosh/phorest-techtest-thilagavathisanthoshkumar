# Implementation Plan: Issue 5, Hardening and quality

Sources:
- `docs/gitHub_issue_bodies.md` (Issue 5, section "Issue 5: Hardening and quality"): goal, tasks, AC-18, AC-19, AC-31, AC-32, AC-33, NFR-04, NFR-05, NFR-07, NFR-09.
- `docs/plans/US-04-rest-api-plan.md`: format and the convention that no `docs/specs/` feature spec exists; the issue body, requirements and design are the spec.
- Existing tests read for coverage (see section 3).

Scope rule: Issue 5 is a test-and-verify issue. No production behaviour change is planned. If a test exposes a defect, fix it in the layer that owns it and record it in section 7.

## 1. Prerequisites

- Issue 4 merged (PR #12 is on `main`).
- Docker running (Testcontainers MySQL). JDK 21 for `./mvnw`.

## 2. Branch

`feature/5-hardening-and-quality` (current branch). PR body: `Closes #5`.

## 3. Coverage already in place vs gaps

| Issue 5 item | Existing coverage | Gap |
|---|---|---|
| Repository: migration | Flyway runs on every Testcontainers test; `JpaMachineRepositoryTest.insertThenFindReturnsTheSameMachine` | None |
| Repository: CHECK constraints (NFR-07) | `SchemaConstraintsTest`: negative float, k<1, k>slots, unknown policy, unknown prize, orphan play/refill | Untested CHECKs: `chk_machine_slot_count`, `chk_machine_play_cost`, `chk_machine_free_plays`, `chk_play_paid`, `chk_play_float_after`, `chk_refill_amount`, `chk_refill_float_after` |
| Repository: FOR UPDATE | `JpaMachineRepositoryTest.deadlockIsReportedAsBusy...`, `lockTimeoutIsReportedAsBusyWithinTwoSeconds` (AC-32 refusal branch, repository level) | None |
| AC-31 machines independent | `MachineServiceTest.playOnOneMachineLeavesAnotherUnchanged` | None |
| AC-18 status follows float, AC-19 refill | `MachineInteractionApiTest.jackpotDrainsMachineAndRefusedPlayChangesNothing`, `refilledMachineAcceptsPlaysAgain`; `MachineHistoryTest` | None (AC-18 asserted in one HTTP test) |
| End-to-end flow (configure, play, drain, refused, refill, play) | Split across the tests above, no single chain | One chained test (Task 2) |
| AC-32 two simultaneous plays | None at service level | Task 1 |
| AC-33 200 parallel plays | None | Task 1 |
| NFR-05 atomicity on failure | `MachineServiceTest.failureAfterTheFloatIsSavedRollsBackTheWholePlay` | None |

## 4. Decisions and scope status

| ID | Decision | Status | Basis |
|---|---|---|---|
| D-q-1 | Concurrency tests go in `MachineServiceTest` (service + real MySQL, same stubbing style as existing tests). No new test class. | In scope | Minimal change; the class already wires service, JDBC, repository spy and spin stub |
| D-q-2 | Concurrent plays use a constant no-win spin (`NO_WIN`) via the existing `spin(...)` stub. Not `ScriptedSpins`. | In scope | `ScriptedSpins` uses an `ArrayDeque` and an unsynchronised `calls++`, so it is not thread-safe |
| D-q-3 | Property-based tests (task 4 in the issue, optional) | **OUT OF SCOPE** | Needs a new dependency (jqwik). Not required by any AC. Lean rule: no dependency for optional work |
| D-q-4 | Service-level test for lock-timeout refusal (AC-32 second half) | **OUT OF SCOPE** (already covered) | `JpaMachineRepositoryTest.lockTimeoutIsReportedAsBusyWithinTwoSeconds` proves the 2 s refusal at the lock layer; the 409 mapping is covered by `ApiExceptionHandlerTest` |
| D-q-5 | "Different machines do not hold each other up" (NFR-04) | **OUT OF SCOPE** for Issue 5 | Not an AC in this issue; AC-31 covers the data-independence part |
| D-q-6 | Load test beyond 200 plays, timing budget assertions | **OUT OF SCOPE** | Not requested; issue measure is exactly 200 plays |
| D-q-7 | Add one HTTP chained flow test rather than changing existing tests | In scope | Issue task 2 |

## 5. Ordered task list (TDD: failing test first, then minimal fix if needed)

### Task 1: concurrency (AC-32, AC-33, NFR-04)

Test file: `src/test/java/com/thilaga/fruitmachine/application/MachineServiceTest.java`

a. **AC-33 `twoHundredParallelPlaysKeepMoneyConsistent`** (new test method)
- Setup: `CONFIG` (MIN_RESERVE, play cost 100p), create machine with float 100,000p (well above 200 × 100p, so the reserve check never refuses). Use `spin(NO_WIN)`. Note: MIN_RESERVE requires float >= 5 × cost, which 100,000p meets.
- Act: `ExecutorService` with 200 tasks (pool size ~ 20), all started behind a `CountDownLatch` start gate; each calls `service.play(id, false)`. Collect futures; `get()` each.
- Assert: all 200 succeed (no `MachineBusyException`); final float + total paid out = start float + 200 × 100p; `countPlays()` = 200. Use `jdbc` to read `total_paid_pence` and sum float.
- Note: with NO_WIN, each play pays 0 and the stake stays in the float, so expected final float = 100,000 + 200 × 100 = 120,000p. Assert that exact value and the 200 play rows.
- Shutdown the executor in `finally`.

b. **AC-32 `twoSimultaneousPlaysSeeEachOthersFloat`** (new test method)
- Setup: float 500p, play cost 100p, `spin(NO_WIN)`. Two tasks behind a start latch.
- Assert: both succeed; the two `floatAfterPence` values are {600, 700} in some order; final float 700p; 2 play rows.

c. Verify: `./mvnw test -Dtest=MachineServiceTest`

Implementation expectation: no production change. If (a) or (b) fails with stale float or lost update, the bug is in `MachineService.play` / `JpaMachineRepository.findForUpdate` (lock held across the whole transaction). Fix there, not in the test.

### Task 2: end-to-end flow over HTTP (AC-18, AC-19, UC-01 to UC-08)

Test file: `src/test/java/com/thilaga/fruitmachine/api/MachineInteractionApiTest.java` (new method, reuses `AbstractApiTest` helpers and `ScriptedSpins`).

a. **`configurePlayDrainRefusedRefillPlayInOneFlow`**
- Configure a one-slot-jackpot MIN_RESERVE machine (certain jackpot, per Issue 6/7 convention).
- Play with JACKPOT spin: expect `prize=JACKPOT`, `floatPence=0`, status NEEDS_REFILL (AC-18 first half).
- Play again: expect 409 `machine-needs-refill`, body has `floatPence=0`, `machineStatus=NEEDS_REFILL`; spin count unchanged (no spin on refusal).
- Refill 5,000p: 200, float 5,000p, status ACTIVE, refill row present (AC-19).
- Final play: 200 with paidOut 5,100p. Corrected at review: a one-slot machine cannot produce NO_WIN, so the last play is another jackpot paying the 5,000p float plus the 100p stake.
- Verify: `./mvnw test -Dtest=MachineInteractionApiTest`

Note: AC-18 says 1,000p refill in the issue text while AC-19 uses 5,000p. Use values from AC-19 for this test (single chain), and the 1,000p case is already asserted in `refilledMachineAcceptsPlaysAgain`. Confirm the 1,000p reading in the requirements before relying on it.

### Task 3: repository CHECK constraints (NFR-07)

Test file: `src/test/java/com/thilaga/fruitmachine/infrastructure/persistence/SchemaConstraintsTest.java`

Add one test per missing CHECK, using the existing `assertRejected(code, insert)` helper and insert helpers. Each test: an otherwise-valid row with one value out of range is rejected by MySQL (error 3819 for CHECK, as the existing tests use).

- `slotCountBelowOneIsRejected` (`chk_machine_slot_count`)
- `playCostOfZeroIsRejected` (`chk_machine_play_cost`)
- `negativeFreePlaysIsRejected` (`chk_machine_free_plays`)
- `negativePaidOnPlayIsRejected` (`chk_play_paid`)
- `negativeFloatAfterOnPlayIsRejected` (`chk_play_float_after`)
- `zeroRefillAmountIsRejected` (`chk_refill_amount`)
- `negativeFloatAfterOnRefillIsRejected` (`chk_refill_float_after`)

Verify: `./mvnw test -Dtest=SchemaConstraintsTest`

Check: each test must fail if its CHECK is removed from `V1__machine_play_refill.sql`. Confirm by temporarily reading the constraint name in the error text, not by editing the migration.

## 6. TDD cycle reminder

Failing test → minimal code (if any) → `./mvnw fmt:format` → commit. One commit per task (Tasks 1, 2, 3), each with `Co-Authored-By` trailer per workflow.

## 7. Test strategy

- Unit: none new (domain already covered).
- Integration (real MySQL via Testcontainers, never H2): Tasks 1 to 3.
- API (MockMvc over the real stack): Task 2.
- Tick off `docs/gitHub_issue_bodies.md` Issue 5 task list as each task commits.

## 8. Definition of Done

- [ ] Task 1: AC-32 and AC-33 tests pass; no `MachineBusyException` in the 200-play run.
- [ ] Task 2: single HTTP chain passes, covering AC-18 and AC-19.
- [ ] Task 3: all CHECK constraints in V1 have a rejection test.
- [ ] `./mvnw verify` green locally with Docker running (format check included).
- [ ] CI green on the PR.
- [ ] `docs/gitHub_issue_bodies.md` Issue 5 boxes ticked.
- [ ] PR body `Closes #5`.

## 9. Risks and open points

- **Flakiness of Task 1 on slow CI.** Each waiting play has a 2 s lock wait (`JpaMachineRepository`). If a single transaction on a loaded runner takes more than ~2 s, later plays are refused as busy and AC-33 fails. Mitigation: keep the executor small (about 20 threads), keep each play tiny, and if it flakes, measure before changing the 2 s constant (NFR-04 fixes it).
- **Connection pool.** Hikari default pool is 10; 200 plays queue on connections. Queue wait is up to Hikari's connection timeout (30 s default), so this does not cause refusal. Confirm no custom pool setting in `application.properties`.
- **Refill amount.** Resolved: Task 2 uses 5,000p (AC-19) per user confirmation. The existing 1,000p test (`refilledMachineAcceptsPlaysAgain`) is kept as is.
- **Plan vs real test coverage.** Section 3 is from reading test method names, not from running coverage. A coverage run (`./mvnw verify` with JaCoCo if present) is not part of this plan.

## 10. Estimated size

3 tasks, about 9 new test methods (2 concurrency, 1 HTTP chain, 7 CHECK tests). No production code expected. No open clarifications. Refill amount confirmed as 5,000p.
