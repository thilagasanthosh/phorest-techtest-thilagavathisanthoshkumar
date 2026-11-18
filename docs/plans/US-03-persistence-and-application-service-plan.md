# Implementation Plan: Issue 3, Persistence and application service

Source: `docs/gitHub_issue_bodies.md` (Issue 3, lines 192-264). Design reference: `docs/03-conceptual-design.docx`, ERD-01 (section 2.3) and section 6 (transactions and locking). No `docs/specs/` feature spec exists, so the issue body is the spec, same as Issues 1 and 2.

## 1. Prerequisites

- Issue 2 (domain core) merged to `main` (PR #9). Confirm `origin/main` contains it before branching.
- Issue 1 infrastructure is in place: `spring-boot-starter-data-jpa`, `spring-boot-starter-flyway`, `flyway-mysql`, `mysql-connector-j`, Testcontainers MySQL 8 (`TestcontainersConfiguration`). No `pom.xml` change expected.

## 2. Branch

`feature/3-persistence-and-application-service` (already checked out). PR body: `Closes #3`.

## 3. Current state and gaps

Already in the domain and reusable:

- `Machine(config, floatPence, freePlayBalance)` rehydration constructor, written for this issue (see its Javadoc).
- `MachineConfig` record (validated). Colour id = position in `colours()`.
- `PlayResult`, `Settlement`, `MachineStatus` (derived, never stored).
- `MachineNeedsRefillException`, `NoFreePlayAvailableException`, `SecureRandomSpinGenerator`, `FixedSpinGenerator`.

Gaps this issue must close:

1. **`Machine` has no id.** Keep the id out of the domain. The application layer generates a UUID and the persistence layer stores it.
2. **Totals are not in the domain.** AC-28 needs total plays and total paid out. ERD-01 stores them on `machine`.
3. **Nominal payout is not returned.** `PayoutSettler` computes `nominal` internally, but `NONE`, `SMALL_PRIZE` and `FULL_HOUSE` audit fields need it (`nominal_pence`, BR-10 "amount that should have been paid"). `Settlement` and `PlayResult` must carry it.
4. **No repository port in the domain.** ERD and section 2.2 say the domain defines `MachineRepository`; JPA implements it.
5. **Refill accepts zero or negative amounts.** `Machine.refill` has no guard. The DB `CHECK float >= 0` does not stop a negative refill that leaves the float non-negative. AC-20 is an Issue 4 test, but the guard belongs in the domain now.
6. **Colour ids vs names.** Domain holds ids. `machine.colours` (JSON) and `play.slots` (JSON) hold names. The mapper converts at the persistence boundary, as `MachineConfig`'s Javadoc says.
7. **`SecureRandomSpinGenerator` sits in `domain`, but the design puts `RandomSpinGenerator` in `infrastructure`.** Section 2.1 and CLD-01 show `RandomSpinGenerator` implementing the domain's `SpinGenerator`. Move the class to `infrastructure` and rename it to `RandomSpinGenerator` (Task 5).
8. **Play check order is reversed.** AD-01 and SD-02 require: machine exists, machine available, float policy, then free-play balance. `Machine.play` (Issue 2 code) checks free-play balance first (lines 28-33). Fix in Task 5.

## 4. Decisions before Task 0

Status: D-a, D-b, D-f use the recommended defaults below unless the user objects (section 9, items 1-3). D-c to D-e are settled in this plan.

- **D-a Nominal payout:** add `nominalPence` to `Settlement` and `PlayResult`. Recommended. No cleaner option exists.
- **D-b Totals:** store `total_plays`, `total_paid_pence`, `total_refilled_pence` on the `machine` row, incremented in the same transaction as each play or refill (ERD-01). Alternative: derive by `SUM`/`COUNT` over the history tables. Recommended: stored, as the design says, so AC-28 reads are O(1).
- **D-c Machine id:** UUID generated in `MachineService.configure`, stored as `BINARY(16)` (ERD-01).
- **D-d Port location:** `domain.MachineRepository` (port, pure Java, uses domain types only) with `JpaMachineRepository` in `infrastructure`, as CLD-01 and section 2.2 show. Domain exceptions `MachineNotFoundException` and `MachineBusyException` live in `domain`, so infrastructure depends only inward.
- **D-e Port methods:** the design lists `findForUpdate`, `save`, `recordPlay`, `recordRefill` (section 2.2, CLD-01). Issue 3 also needs `insert` (UC-01 creates a machine) and `find` (UC-06 reads without a lock). These two are additions, not changes. `save` and `recordPlay` gain the machine id, and `recordPlay` gains `usedFreePlay` because ERD `play.used_free_play` needs it. The read model is `domain.MachineState` (id, `Machine`, totals).
- **D-f Refill guard:** `Machine.refill` rejects `amountPence <= 0` with `IllegalArgumentException`. This is UC-08 step 2, so it belongs to the refill flow in this issue. AC-20 is only the API-level test in Issue 4.

## 5. Ordered tasks

TDD cycle for every task: failing test, minimal code, `./mvnw fmt:format`, `./mvnw verify`, commit.

Test naming: the pom has no Failsafe plugin, so Surefire runs only `*Test` / `Test*` / `*Tests`. Name integration tests `*Test`, not `*IT`.

Integration tests: `@SpringBootTest` + `@Import(TestcontainersConfiguration.class)`, real MySQL 8. Service tests must **not** be `@Transactional`, so the service's own transaction is exercised. Clear tables in `@BeforeEach` in FK order: `play`, `refill`, `machine`.

### Task 0: Domain prep (nominal payout, refill guard)

- **Satisfies:** BR-10 (fields), NFR-03, D-a, D-f.
- **Files:** `domain/Settlement.java` (+`nominalPence`), `domain/PayoutSettler.java` (populate it), `domain/PlayResult.java` (+`nominalPence`, update equals/hashCode/toString), `domain/Machine.java` (pass nominal through; guard in `refill`).
- **Test first:**
  - `PayoutSettlerTest`: assert `nominalPence` for JACKPOT, FULL_HOUSE, SMALL_PRIZE, NONE.
  - `MachineTest`: `refill(0)` and `refill(-1)` throw `IllegalArgumentException` and leave the float unchanged.
  - `PlayResultTest`: equality includes `nominalPence`.
- **Minimal code:** as listed. Existing Issue 2 assertions keep passing; only constructor call sites change.
- **Verify:** `./mvnw test -Dtest=PayoutSettlerTest,MachineTest,PlayResultTest`
- **Commit:** `Expose nominal payout and reject non-positive refills`

### Task 1: Flyway V1 schema

- **Satisfies:** NFR-06, NFR-07, NFR-08 (schema part).
- **Files:**
  - `src/main/resources/db/migration/V1__machine_play_refill.sql`
  - `src/main/resources/application.properties`: add `spring.jpa.hibernate.ddl-auto=validate` (ERD: the app only validates the schema at start-up).
- **Schema (ERD-01):**
  - `machine`: `id BINARY(16) PK`, `slot_count INT`, `colours JSON`, `k INT`, `play_cost_pence BIGINT`, `float_policy VARCHAR(20)`, `float_pence BIGINT`, `free_plays INT`, `total_plays BIGINT`, `total_paid_pence BIGINT`, `total_refilled_pence BIGINT`, `version BIGINT NOT NULL`, `created_at`, `updated_at`.
  - `machine` CHECKs: `slot_count >= 1`, `1 <= k <= slot_count`, `play_cost_pence > 0`, `float_pence >= 0`, `free_plays >= 0`, `float_policy IN ('MIN_RESERVE','NONE')`. NOT NULL on all columns (ERD-01).
  - `play`: `id BINARY(16) PK`, `machine_id BINARY(16) FK -> machine`, `played_at`, `slots JSON`, `prize VARCHAR(20)`, `used_free_play BOOLEAN`, `nominal_pence BIGINT`, `paid_pence BIGINT`, `free_plays_credited INT`, `float_after_pence BIGINT`.
  - `play` CHECKs: `prize IN ('JACKPOT','FULL_HOUSE','SMALL_PRIZE','NONE')`, `paid_pence >= 0`, `float_after_pence >= 0`.
  - `refill`: `id BINARY(16) PK`, `machine_id BINARY(16) FK -> machine`, `refilled_at`, `amount_pence BIGINT`, `float_after_pence BIGINT`. CHECKs: `amount_pence > 0`, `float_after_pence >= 0`.
- **Test first** (`persistence/SchemaConstraintsTest`, `JdbcTemplate` inserts): each of these throws `DataIntegrityViolationException`: negative float, `k = 0`, `k > slot_count`, unknown prize, unknown float policy, orphan `play.machine_id`. This is the NFR-07 "store rejects invalid state" check.
- **Verify:** `./mvnw test -Dtest=SchemaConstraintsTest`
- **Commit:** `Add Flyway V1 schema for machine, play and refill`

### Task 2: JPA entities and MachineMapper

- **Satisfies:** NFR-06 (round trip), BR-11.
- **Files:** `infrastructure/persistence/MachineEntity.java` (`@Version long version`, `UUID id`, `colours` as JSON `List<String>`), `PlayEntity.java`, `RefillEntity.java`, `MachineMapper.java`.
- **Mapper:** `MachineEntity -> Machine` (builds `MachineConfig` with names, restores float and free plays); `Machine + config -> entity`; colour id <-> name for `slots`.
- **Test first** (`persistence/MachineMapperTest`, plain JUnit, no Spring): round trip of config, float and free plays; spin ids `[0,2,2,3]` map to names `[black, green, green, yellow]` and back.
- **Verify:** `./mvnw test -Dtest=MachineMapperTest`
- **Commit:** `Add JPA entities and MachineMapper`

### Task 3: Domain port and JpaMachineRepository (lock, timeout)

- **Satisfies:** NFR-04 (lock), UC-12, D-d, D-e.
- **Files:**
  - `domain/MachineRepository.java`: `UUID insert(MachineConfig, long floatPence, long freePlays)` (addition, D-e); `Optional<MachineState> find(UUID)` (addition, D-e); `Optional<Machine> findForUpdate(UUID)`; `void save(UUID, Machine)`; `void recordPlay(UUID, PlayResult, boolean usedFreePlay)`; `void recordRefill(UUID, long amountPence, long floatAfterPence)`.
  - `domain/MachineState.java` (record: id, `Machine`, totals).
  - `domain/MachineNotFoundException.java`, `domain/MachineBusyException.java`.
  - `infrastructure/persistence/JpaMachineRepository.java` (implements the port) and a Spring Data interface with `@Lock(PESSIMISTIC_WRITE)` and `@QueryHints(jakarta.persistence.lock.timeout = 2000)` on the lookup.
- **Behaviour:** `findForUpdate` translates lock timeout (`PessimisticLockException`, `PessimisticLockingFailureException`) and deadlock (MySQL error 1213, `CannotAcquireLockException`) into `MachineBusyException`. Section 6.2 maps both to the same busy response (UC-12). Translate **inside the repository**, so the service's transaction still rolls back.
- **Test first** (`persistence/JpaMachineRepositoryTest`):
  - insert then find round trip; `findForUpdate` of unknown id is empty.
  - deadlock: two transactions locking two machines in opposite order produce `MachineBusyException` for the loser, not a generic 500 (section 6.2).
  - lock timeout: in a second thread, open `TransactionTemplate`, call `findForUpdate` on the same id, hold the lock; the main thread's `findForUpdate` throws `MachineBusyException` in about 2 s (assert under 5 s).
- **Risk check:** confirm the timeout hint is honoured on MySQL 8 by this test. If Hibernate ignores it, set `innodb_lock_wait_timeout` on the session and note the change in the code.
- **Verify:** `./mvnw test -Dtest=JpaMachineRepositoryTest`
- **Commit:** `Add JpaMachineRepository with pessimistic lock and timeout`

### Task 4: MachineService configure and read

- **Satisfies:** AC-01, AC-28, AC-29 (read path).
- **Files:** `application/MachineService.java` (`@Service`, `@Transactional`): `UUID configure(MachineConfig)` builds `new Machine(config)`, calls `insert`; `MachineState read(UUID)` calls `find`, throws `MachineNotFoundException` when empty. Reserve value: reuse the D10 reserve multiplier already in the domain. If `FloatPolicy` does not expose the reserve amount, add one method. Check before adding.
- **Test first** (`application/MachineServiceTest`):
  - AC-01: 4 slots, colours black/white/green/yellow, k = 2, cost 100p, float 1,000p, MIN_RESERVE gives an ACTIVE machine with a new UUID, reserve 500p.
  - UC-01 A2: a `NONE` policy machine is accepted with a starting float below the reserve; status is ACTIVE.
  - UC-01 postcondition: 0 free plays, 0 plays, 0 total paid out.
  - AC-28: read returns config, float, reserve, free plays, total plays, total paid, status.
  - AC-29: read of unknown id throws `MachineNotFoundException`.
- **Verify:** `./mvnw test -Dtest=MachineServiceTest`
- **Commit:** `Add MachineService configure and read`

### Task 5: MachineService.play (one transaction)

- **Satisfies:** BR-02, BR-10, NFR-05, AC-29 (play path), AC-30 (play part), AC-31.
- **Files:**
  - `application/MachineService.java`: `PlayResult play(UUID id, boolean useFreePlay)`, `@Transactional`:
    1. `findForUpdate(id)`, or `MachineNotFoundException`.
    2. `machine.play(useFreePlay, spinGenerator)`. Refusals throw before any write.
    3. `save(id, machine)`.
    4. `recordPlay(id, result, useFreePlay)`.
  - `infrastructure/config/GameConfig.java`: `@Bean SpinGenerator` returning `RandomSpinGenerator`.
  - Move `domain/SecureRandomSpinGenerator.java` to `infrastructure/RandomSpinGenerator.java` (renamed, per CLD-01 and section 2.1). Update its `SpinGeneratorTest` references; `FixedSpinGenerator` stays in `domain` because tests use it as a test double.
  - Reorder checks in `domain/Machine.play` to match AD-01 and SD-02: float policy (`checkCanPlay`) first, then free-play balance. Test first in `MachineTest`: a free play with no credit and float below reserve throws `MachineNeedsRefillException`, not `NoFreePlayAvailableException`.
- **Test first** (`application/MachineServiceTest`, same class, `@MockitoBean SpinGenerator` for forced spins; test scope only):
  - paid jackpot on a four-of-a-kind spin: one play row with `prize = JACKPOT`, `float_after = 0`, `paid = float`.
  - paid no-win (UC-02 A1): nothing paid, float equals float after stake, one play row with `prize = NONE`, `paid = 0`, `nominal = 0`.
  - refused play (float 0): throws `MachineNeedsRefillException`; play count unchanged (AC-30 refused).
  - rollback: `@MockitoSpyBean MachineRepository` made to throw in `recordPlay` after `save` has run. Assert float and free plays unchanged afterwards (NFR-05).
  - free play with a credit: seed a machine with free plays via `insert`; one credit used, no stake, play row `used_free_play = true`.
  - free play with zero balance: `NoFreePlayAvailableException`, nothing changes (AC-26 is Issue 2; this is the persisted check).
  - unknown machine: `MachineNotFoundException`, nothing changes (AC-29).
  - AC-31: plays on machine A leave machine B's float, free plays and history unchanged.
- **Verify:** `./mvnw test -Dtest=MachineServiceTest`
- **Commit:** `Add MachineService.play with one transaction and history record`

### Task 6: MachineService.refill

- **Satisfies:** AC-19, BR-08, BR-10, D11.
- **Files:** `application/MachineService.java`: `void refill(UUID id, long amountPence)`, `@Transactional`: `findForUpdate`, `machine.refill(amount)` (guard from Task 0), `save`, `recordRefill(id, amount, floatAfter)`.
- **Test first** (`MachineServiceTest`):
  - AC-19: float 0, refill 5,000p gives float 5,000p, one refill row, status ACTIVE.
  - BR-08: a machine refused for low float (float 0) is refused by `play`, then refilled to exactly the reserve (500p) and accepted again.
  - UC-08 A1: a refill that leaves the float below the reserve is accepted; status stays `NEEDS_REFILL` and `play` is still refused.
  - unknown machine: `MachineNotFoundException`.
  - refill of 0 or negative: `IllegalArgumentException`, no refill row.
- **Verify:** `./mvnw test -Dtest=MachineServiceTest`
- **Commit:** `Add MachineService.refill with audit record`

### Task 7: History counts and restart survival

- **Satisfies:** AC-30 (full scenario), NFR-06, NFR-08, Done-when (restart).
- **Test first** (`application/MachineHistoryTest`):
  - AC-30: on one machine, 3 successful plays, 1 refused play, 1 refill. Assert exactly 3 play rows and 1 refill row, each with the BR-10 fields; totals on `machine` match.
  - Restart: create and play a machine in one `ApplicationContext`, close it, start a second context on the same Testcontainers MySQL URL, read the machine. Assert float, free plays, totals and history are identical. This is a real restart of the Spring context against a persistent database, not a rerun in the same context.
- **Verify:** `./mvnw test -Dtest=MachineHistoryTest`
- **Commit:** `Add history count and restart survival tests`

## 6. Test strategy

- **Unit, no Spring or JPA:** `PayoutSettlerTest`, `MachineTest`, `PlayResultTest`, `MachineMapperTest`.
- **Integration, real MySQL 8 via Testcontainers:** `SchemaConstraintsTest`, `JpaMachineRepositoryTest`, `MachineServiceTest`, `MachineHistoryTest`.
- **Forced spins:** `@MockitoBean` / `@MockitoSpyBean` in tests only. Nothing test-only goes into `src/main` config.
- **Out of scope here:** concurrency AC-32 and AC-33 (Issue 5), API error mapping and refill input validation AC-20 and AC-21 (Issue 4), Compose and documentation (Issue 6).

## 7. Definition of Done

- [ ] Tasks 0-7 committed, each test first
- [ ] AC-01, AC-19, AC-28, AC-29, AC-30, AC-31 pass as tests on MySQL
- [ ] Schema rejects negative float, bad `k`, unknown prize or policy, and orphan play or refill rows
- [ ] A refused or failed play leaves float, free plays and history unchanged
- [ ] Lock timeout produces `MachineBusyException` in about 2 s
- [ ] No Spring or JPA imports in `domain` (review check)
- [ ] `./mvnw verify` green with Docker running; formatter clean
- [ ] PR body contains `Closes #3`

## 8. Risks

- **Lock-timeout hint:** Hibernate may not apply `jakarta.persistence.lock.timeout` on MySQL. Caught by Task 3's test; fallback in Task 3.
- **Domain change ripple:** adding `nominalPence` to `PlayResult` and `Settlement` touches Issue 2 equality tests. Additive, but expect edits.
- **Check-order change touches Issue 2 tests (user confirmed, section 9 item 5):** reordering `Machine.play` changes which exception a free play with low float raises. Existing `MachineTest` cases for that combination need updating.
- **Optimistic `@Version` with a pessimistic lock:** `save` must not hit a stale version inside the same transaction. Covered by the Task 5 tests.
- **Totals drift:** totals are updated in `recordPlay` and `recordRefill`, inside the same transaction. A missed update would not be caught by the lock. Covered by AC-30 totals assertion in Task 7.
- **Restart test:** a second `ApplicationContext` against the same container is less than a full process restart. The true restart check belongs to Issue 6/7.
- **Colour names in JSON:** `play.slots` stores names, so renaming a colour in `machine.colours` later would not rewrite history. Acceptable: history is a record of what was shown.

## 9. Clarifications before starting

1. Open, default applies: D-b stored totals (ERD-01) rather than derived sums.
2. Open, default applies: D-a nominal exposed on `PlayResult` and `Settlement`.
3. Open, default applies: D-f refill guard in the domain, per UC-08 step 2.
4. Open, default applies: Task 7's restart approach (second context, same container) is the Issue 3 restart check.
5. CONFIRMED (user, 2026-11-18): move and rename `SecureRandomSpinGenerator` to `RandomSpinGenerator` in `infrastructure` (Task 5), and reorder the checks in `Machine.play` so the float policy runs before the free-play balance check (Task 5, including the Issue 2 `MachineTest` update).

## 10. Summary

- **Estimated tasks:** 8 (Tasks 0-7), all TDD.
- **Risks:** section 8. Main ones are the lock-timeout hint and the `PlayResult` change ripple.
- **Open questions:** section 9, items 1-4 (defaults apply). Item 5 is confirmed.
- **Ready for Task 0:** yes, pending a go-ahead to implement.

## 11. Implementation deviations (as built)

- **Starting float column:** `machine.starting_float_pence` added to V1. ERD-01 has no column, but AC-28 (read returns config) and the AC-33 invariant (`float + total paid = starting float + 200 x play cost`) need it.
- **Insert signature:** `MachineRepository.insert(UUID id, Machine machine)` returns void. The id comes from `MachineService.configure` (D-c), so the port does not generate it.
- **Lock timeout:** Hibernate drops `jakarta.persistence.lock.timeout` on MySQL, and MySQL 8.4 ignores `SET_VAR(innodb_lock_wait_timeout)` (probed). `findForUpdate` runs `SET SESSION innodb_lock_wait_timeout = 2`, the locking `SELECT ... FOR UPDATE`, then `SET SESSION ... = DEFAULT`. Two extra statements per play and refill.
- **Busy mapping:** `findForUpdate` catches `PersistenceException` and maps MySQL 1205 (lock wait timeout) and 1213 (deadlock) to `MachineBusyException`. Catching only Spring's `PessimisticLockingFailureException` missed the deadlock, because the translation happens after the method returns.
- **Extra domain types:** `MachineTotals` and `MachineState` (Task 3), `Machine.reservePence()` and `FloatPolicy.reservePence()` (Task 4).
- **Test visibility:** `TestcontainersConfiguration` made `public` so tests in other packages can import it.
- **Test packages:** persistence tests live in `infrastructure.persistence`, and `RandomSpinGeneratorTest` in `infrastructure`.
- **Restart test:** a second `ApplicationContext` on the same container, as Task 7 describes. It is not a process restart.
