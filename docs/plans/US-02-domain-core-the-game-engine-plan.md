# Implementation Plan: Issue 2, Domain core: the game engine

Source: `docs/gitHub_issue_bodies.md` (Issue 2, lines 33-191). No separate feature spec exists in `docs/specs/`; the issue body is used as the spec, same as Issue 1.

## 1. Prerequisites

Issue 1 is merged (PR #8, merged into `main` as `5ce50ca`). This branch (`feature/2-domain-core-the-game-engine`) was cut from the tip of `feature/1-project-foundation`, whose tree is identical to the merged `main` (`git diff 01c95e4 5ce50ca --stat` is empty), so no rebase is needed before starting. Local `main` itself is still stale (12 commits behind `origin/main`) — run `git fetch && git checkout main && git merge --ff-only origin/main` at some point for hygiene; it doesn't block this branch.

## 2. Branch

`feature/2-domain-core-the-game-engine` (already checked out, per the issue's stated branch name). PR body: `Closes #2`.

## 3. Scope and current state

Issue 2 is pure Java domain logic: no Spring, no JPA, no database. "Done when: All listed AC pass as unit tests with no database or Spring." The `domain` package currently holds only `package-info.java` ("Game rules in pure Java. No Spring or JPA.") — everything below is new.

Decisions D1-D10 (full text in the issue, reproduced in CLAUDE.md) are settled and drive the design directly:

- D2/D7: the stake joins the float before evaluation on a paid play; a free play adds no stake.
- D3: free plays = ceil(shortfall ÷ play cost); float is paid down to 0.
- D4/D5: money is `long` pence; half of an odd float rounds down (integer division).
- D6: full house is impossible when slots > colours — this falls out naturally from pigeonhole (you cannot have `slots` distinct colours from a smaller pool), so no special-case code is needed, only a boundary test (AC-13).
- D9: a jackpot sets the float to 0 — also a natural consequence of "jackpot pays the entire float", not special-cased logic.
- D10: MIN_RESERVE refuses plays (paid or free) while float < 5 × play cost.

### Assumptions to confirm (ambiguous or unstated in the issue)

1. **Colour representation — confirmed, not just assumed.** `docs/03-conceptual-design.docx` (ERD-01, and §7.1: "Colour ids are small integers in the hot path; names appear only at the API edge") settles this: `MachineConfig.colours()` is the ordered `List<String>` of distinct names, and its **position is the colour id** — no separate mapping class exists anywhere in the design. `SpinGenerator`, `SpinProfile`, `PrizeRule`/`PrizeEvaluator` and `Machine` only ever see `colourCount = config.colours().size()` and `int` ids; converting an id back to a name is Issue 3's `MachineMapper` (writing the `play.slots` JSON column) and Issue 4's response DTOs — never Issue 2's domain. AC-05/AC-34/AC-35's large-colour-count scenarios still mean the **test code** invents synthetic names (e.g. `"C0".."C299"`) to populate that list.
2. **Valid range for `k` (small-prize run length) — corrected from the first draft of this plan.** The same design doc's ERD-01 states the database `CHECK` constraint as **`1 <= k <= slots`** (not `2 <= k <= slots`, which this plan originally guessed from the AC examples alone). The issue's AC-03 only tests `k = 0` (invalid) and `k = 5` with 4 slots (invalid, `k > slots`); `k = 1` is valid per the source design even though it means every spin wins at least a small prize. **Plan now uses `1 <= k <= slots`.**
3. **No upper-bound validation for slot/colour counts.** BR-12 states the rules must *work correctly* for up to 10,000 slots and up to 1,000 colours — that's a performance envelope (NFR-01, AC-35), not a stated validation rule, and no AC asks for a config above those numbers to be refused. Not implementing a rejection for it; flag if this is wrong.
4. **New machine starts with 0 free plays.** Not stated in the issue; it's the only sensible default and nothing contradicts it.
5. **`FixedSpinGenerator` lives in `src/main`, not `src/test`.** CLAUDE.md's architecture section lists it as part of what the `domain` package holds ("a `SpinGenerator` interface (secure-random impl; scripted/fixed impl for tests)"), so it ships in production code as a test-double *type*, even though nothing in Issue 2 wires it into a running Spring context. CLAUDE.md's "forced-spin system-test configuration must be test-only and never active in production" rule is Issue 7's concern (wiring it via config); Issue 2 only needs the class to exist so unit tests can force spins (NFR-09).

## 4. Ordered tasks

TDD cycle for every task: failing test → minimal code → `./mvnw fmt:format` → `./mvnw verify` → commit. All tests here are plain JUnit 5 unit tests — no `@SpringBootTest`, no Testcontainers, consistent with "Done when: ... as unit tests with no database or Spring."

### Task 1: MachineConfig (validation + defaults)

- **Satisfies:** AC-02, AC-03, AC-04, AC-05, BR-12.
- **Files:**
  - `domain/MachineConfig.java` — immutable value type: `slots`, `colours` (`List<String>`), `k`, `playCostPence` (`long`), `startingFloatPence` (`long`), `floatPolicy` (the `FloatPolicy` from Task 6 — see note below on ordering).
  - `domain/InvalidMachineConfigException.java` — carries `List<String>` of faulty-field messages (AC-03 needs "a list of the faulty fields").
- **Test first** (`domain/MachineConfigTest.java`):
  - `defaultsApply_whenOnlyCostAndFloatGiven()` — AC-02: construct with only play cost 100p and float 1,000p; assert slots=4, colours=`["black","white","green","yellow"]`, k=2, policy is MIN_RESERVE.
  - `rejectsInvalidSettings_withFaultyFieldList()` — AC-03: four sub-cases (k=0; k=5 with 4 slots; duplicate colours; play cost 0; negative float) each throw `InvalidMachineConfigException` naming the offending field, and no `MachineConfig` instance is produced.
  - `rejectsStartingFloatBelowReserve_acceptsAtReserve()` — AC-04: play cost 100p, MIN_RESERVE; float 499p refused, 500p accepted.
  - `acceptsLargeExplicitConfig()` — AC-05: 10 slots, 300 colours (synthetic names), k=3, play cost 50p, float 1,000p — constructed exactly as given.
- **Minimal code:** a static factory (e.g. `MachineConfig.of(...)`) that validates then constructs; a `MachineConfig.withDefaults(playCostPence, startingFloatPence)` overload for AC-02. Forward-reference `FloatPolicy` as an interface type only (Task 6 supplies implementations) — or, simpler, give `MachineConfig` a `FloatPolicyType` enum (`MIN_RESERVE`, `NONE`) field instead of holding a `FloatPolicy` instance, and let `Machine` (Task 7) resolve the enum to a `FloatPolicy` implementation. **Decision: use the enum**, to avoid a circular/forward dependency between Tasks 1 and 6. Record this choice when it's made.
- **Verify:** `./mvnw test -Dtest=MachineConfigTest`.
- **Commit:** `Add MachineConfig with validation and defaults`

### Task 2: SpinGenerator

- **Satisfies:** AC-34, NFR-09, NFR-15.
- **Files:**
  - `domain/SpinGenerator.java` — interface, e.g. `int[] generate(int slots, int colours)` returning a colour index per slot.
  - `domain/SecureRandomSpinGenerator.java` — `java.security.SecureRandom`-backed implementation.
  - `domain/FixedSpinGenerator.java` — returns a pre-set `int[]` (or a queue of them) regardless of `slots`/`colours`, to force a spin in a test.
- **Test first** (`domain/SpinGeneratorTest.java`):
  - `fixedGenerator_returnsExactlyWhatWasConfigured()` — sanity check used by every later rule/machine test.
  - `secureRandomGenerator_isFairOverLargeSample()` — AC-34: 4 slots, 4 colours, large sample (e.g. 100,000 spins); assert each colour's observed frequency per slot is within a stated tolerance of 25% (pick a tolerance loose enough not to flake, e.g. ±2 percentage points, and say so in the test name/comment since this is inherently statistical).
- **Minimal code:** the two implementations.
- **Verify:** `./mvnw test -Dtest=SpinGeneratorTest`.
- **Commit:** `Add SpinGenerator with secure-random and fixed implementations`

### Task 3: SpinProfile (one-pass evaluation)

- **Satisfies:** AC-06, AC-13, AC-14, AC-35, NFR-01.
- **Files:** `domain/SpinProfile.java` — computed once from a spin (`int[]`): `longestRun` (longest run of adjacent equal colours) and `distinctColours` (count of distinct colour indices present). Single pass: track previous colour and current-run length for `longestRun`; use a `BitSet` (sized by colour count, set once per slot) for `distinctColours` so the loop body touches O(1) state per slot regardless of colour count.
- **Test first** (`domain/SpinProfileTest.java`):
  - `runLengthBoundary_exactlyKWinsOneBelowDoesNot()` — AC-06: with k=3, a spin with a run of exactly 3 reports `longestRun == 3`; a spin with only runs of 2 reports `longestRun == 2`. (The small-prize *decision* is PrizeRule's job — Task 4 — this test only checks the profile's numbers.)
  - `fullHouseCountCannotExceedColourPool()` — AC-13: 5 slots, 4 colours; for any spin, `distinctColours <= 4 < 5`, so `distinctColours == slots` (full house) is structurally impossible. Assert over a generated/forced spin that it never reports `distinctColours == 5`.
  - `oneSlotSpin_isTriviallyAllSameColour()` — AC-14: 1 slot; `longestRun == 1 == slots` and `distinctColours == 1`.
  - `largeSpinEvaluatesUnder100ms()` — AC-35, NFR-01: 1,000,000 slots; assert wall-clock time for `SpinProfile.of(spin)` is under 100 ms. Use `System.nanoTime()`, not a mocked clock; allow a little headroom in the assertion given CI variance, but keep the 100 ms ceiling from the AC.
- **Minimal code:** the one-pass computation described above.
- **Verify:** `./mvnw test -Dtest=SpinProfileTest`.
- **Commit:** `Add one-pass SpinProfile`

### Task 4: PrizeRule and PrizeEvaluator

- **Satisfies:** AC-07, AC-08, AC-09, AC-10, AC-11, AC-13, AC-14, BR-04, NFR-11.
- **Files:**
  - `domain/PrizeType.java` — enum `JACKPOT, FULL_HOUSE, SMALL_PRIZE, NONE`.
  - `domain/PrizeRule.java` — interface, e.g. `Optional<PrizeType> evaluate(SpinProfile profile, MachineConfig config)`.
  - `domain/JackpotRule.java` — matches when `profile.distinctColours() == 1`.
  - `domain/FullHouseRule.java` — matches when `profile.distinctColours() == config.slots()` (naturally never true when `slots > colours.size()`, covering AC-13/D6 without extra code).
  - `domain/SmallPrizeRule.java` — matches when `profile.longestRun() >= config.k()`.
  - `domain/PrizeEvaluator.java` — holds an **ordered** `List<PrizeRule>` (`Jackpot, FullHouse, SmallPrize` — this order is what makes "first match wins" equal "highest priority wins", per D1); returns `PrizeType.NONE` if nothing matches.
- **Test first:**
  - `JackpotRuleTest` / `FullHouseRuleTest` / `SmallPrizeRuleTest` — one test per rule, each using a `FixedSpinGenerator`-produced spin, asserting the rule matches or doesn't.
  - `PrizeEvaluatorTest.onlyHighestPrizePays()` — AC-11: a forced spin of four identical colours, which (incidentally) also contains runs ≥ k; assert the evaluator returns `JACKPOT`, not `SMALL_PRIZE`.
  - `PrizeEvaluatorTest.noWinWhenNoRuleMatches()` — AC-10.
  - `PrizeEvaluatorTest.oneSlotIsAlwaysJackpot()` — AC-14, via the evaluator rather than the raw profile.
- **Minimal code:** the four classes above; `PrizeEvaluator`'s rule list is the one place a new rule (NFR-11) gets registered.
- **Verify:** `./mvnw test -Dtest=JackpotRuleTest,FullHouseRuleTest,SmallPrizeRuleTest,PrizeEvaluatorTest`.
- **Commit:** `Add PrizeRule implementations and PrizeEvaluator`

### Task 5: PayoutSettler

- **Satisfies:** AC-07, AC-08, AC-09, AC-10, AC-12, AC-22, AC-23, AC-24, BR-05, BR-06, D2, D3, D5, D9, NFR-03.
- **Files:**
  - `domain/Settlement.java` — value record: `paidPence`, `freePlaysEarned`, `floatAfterPence`.
  - `domain/PayoutSettler.java` — `Settlement settle(long floatBeforeStakePence, long stakePence, long playCostPence, PrizeType prize)`:
    1. `floatAfterStake = floatBeforeStakePence + stakePence` (D2; `stakePence` is 0 for a free play per D7 — the caller, Task 7, decides the stake, not this class).
    2. `nominal` by prize type: `JACKPOT` → `floatAfterStake`; `FULL_HOUSE` → `floatAfterStake / 2` (integer division = round down, D5); `SMALL_PRIZE` → `5 * playCostPence`; `NONE` → `0`.
    3. `paid = Math.min(nominal, floatAfterStake)`.
    4. `shortfall = nominal - paid`.
    5. `freePlaysEarned = (prize == JACKPOT) ? 0 : ceilDiv(shortfall, playCostPence)` (D3; the jackpot branch is defensive — step 1-3 already make `shortfall` mathematically 0 for a jackpot since `nominal == floatAfterStake == paid`, but the issue calls out BR-06/AC-24 explicitly, so keep the explicit guard rather than rely on the arithmetic alone).
    6. `floatAfter = floatAfterStake - paid`.
- **Test first** (`domain/PayoutSettlerTest.java`): one test per AC above, each asserting `paidPence`, `freePlaysEarned` and `floatAfterPence` against the issue's worked numbers (e.g. AC-12: float 1,001p stake joins to 1,101p, full house pays 550p, 551p remains).
- **Minimal code:** as described; a small `ceilDiv(long shortfall, long cost)` helper (`(shortfall + cost - 1) / cost`, with `shortfall == 0` short-circuited to 0 to avoid the no-op but harmless `+cost-1` when there's nothing owed).
- **Verify:** `./mvnw test -Dtest=PayoutSettlerTest`.
- **Commit:** `Add PayoutSettler`

### Task 6: FloatPolicy

- **Satisfies:** AC-16, AC-17, AC-18, AC-27, BR-07, D10.
- **Files:**
  - `domain/MachineStatus.java` — enum `ACTIVE, NEEDS_REFILL`.
  - `domain/FloatPolicy.java` — interface: `boolean canPlay(long floatPence, long playCostPence)`, `MachineStatus status(long floatPence, long playCostPence)`.
  - `domain/MinimumReservePolicy.java` — `canPlay` / `status` both false/`NEEDS_REFILL` when `floatPence < 5 * playCostPence`.
  - `domain/NoReservePolicy.java` — `canPlay` always true; `status` always `ACTIVE` (NONE policy has no reserve concept to report against).
  - Resolve `MachineConfig`'s `FloatPolicyType` enum (Task 1) to a `FloatPolicy` instance here, e.g. a small `FloatPolicyType#toPolicy()` or a switch in `Machine` (Task 7) — pick whichever keeps `MachineConfig` free of behaviour; **decision: a package-private factory method, not a switch repeated in `Machine`**, so a future third policy is one new class plus one line in the factory.
- **Test first** (`domain/MinimumReservePolicyTest.java`, `domain/NoReservePolicyTest.java`):
  - AC-16: float 499p, play cost 100p → `canPlay == false`, `status == NEEDS_REFILL`.
  - AC-17: float 500p, play cost 100p → `canPlay == true`, `status == ACTIVE`.
  - AC-18: float 0p → `NEEDS_REFILL`; after refilling to 1,000p → `ACTIVE` (this test exercises the policy function twice with two float values; `Machine.refill` itself is Task 7).
  - AC-27 is a `Machine`-level test (Task 7) since it's about a free-play credit surviving a refusal — `FloatPolicy` itself has no concept of credits.
- **Minimal code:** as described.
- **Verify:** `./mvnw test -Dtest=MinimumReservePolicyTest,NoReservePolicyTest`.
- **Commit:** `Add FloatPolicy implementations`

### Task 7: Machine aggregate

- **Satisfies:** AC-15, AC-16, AC-17, AC-18, AC-22, AC-23, AC-24, AC-25, AC-26, AC-27 — the task that wires every earlier piece together.
- **Files:**
  - `domain/PlayResult.java` — value record: `int[] spin`, `PrizeType prize`, `long paidPence`, `long freePlaysEarned`, `long freePlayBalanceAfter`, `long floatAfterPence`, `MachineStatus status` (AC-15's required response shape).
  - `domain/Machine.java` — aggregate holding `MachineConfig config`, `FloatPolicy policy` (resolved from `config`'s policy type), `long floatPence`, `long freePlayBalance` (starts at 0 — see Assumption 4).
    - `PlayResult play(boolean useFreePlay, SpinGenerator generator)`:
      1. If `useFreePlay` and `freePlayBalance == 0` → reject (throw, e.g. `NoFreePlayAvailableException`) — AC-26. No spin, no state change.
      2. If `!policy.canPlay(floatPence, config.playCostPence())` → reject (e.g. `MachineNeedsRefillException`) — AC-16, AC-27. This check applies whether or not `useFreePlay` is set (BR-07 says "any play"), and runs *before* the credit is consumed, so a rejected free-play attempt leaves the balance untouched (AC-27).
      3. If `useFreePlay`, decrement `freePlayBalance` by 1 now (D7 — "one credit is used").
      4. `stake = useFreePlay ? 0 : config.playCostPence()`.
      5. `spin = generator.generate(config.slots(), config.colours().size())`.
      6. `profile = SpinProfile.of(spin)`; `prize = PrizeEvaluator.evaluate(profile, config)`.
      7. `settlement = PayoutSettler.settle(floatPence, stake, config.playCostPence(), prize)`.
      8. `floatPence = settlement.floatAfterPence()`; `freePlayBalance += settlement.freePlaysEarned()`.
      9. Return a `PlayResult` built from the above plus `policy.status(floatPence, config.playCostPence())`.
    - `void refill(long amountPence)`: `floatPence += amountPence` (AC-18).
- **Test first** (`domain/MachineTest.java`) — one method per AC, each building a `Machine` with a `FixedSpinGenerator` forcing the exact spin the AC describes:
  - `paidPlayReturnsFullResultShape()` — AC-15.
  - `rejectsPlayBelowReserve_noStateChange()` — AC-16: assert float/balance/no-history-entry (history itself is Issue 3; here just assert float and balance are unchanged and an exception is thrown).
  - `acceptsPlayAtExactlyTheReserve()` — AC-17.
  - `statusFollowsFloat_afterJackpotThenRefill()` — AC-18.
  - `noneScenarios` — AC-22, AC-23, AC-24, AC-25 (shortfall → free plays; shortfall rounds up; jackpot never creates free plays; using a free-play credit end-to-end with the `1 - 1 + 5 = 5` balance arithmetic from the issue).
  - `rejectsFreePlayWithoutCredit()` — AC-26.
  - `heldFreePlaySurvivesARefusal()` — AC-27: free play attempted below reserve (refused, balance still 1), operator refills, same free play now accepted.
- **Minimal code:** as described above.
- **Verify:** `./mvnw test -Dtest=MachineTest`, then `./mvnw verify` for the full module.
- **Commit:** `Add Machine aggregate`

## 5. Test strategy

- **Unit only**, per the issue's "Done when": plain JUnit 5, no `@SpringBootTest`, no Testcontainers. Every class above is pure Java with no Spring/JPA import (verify by eye in review — still no automated import check, per Issue 1's open point).
- **Forced spins:** every AC that depends on a specific spin result (AC-07 through AC-14, AC-22 through AC-27) uses `FixedSpinGenerator`, never the secure-random one — this is exactly NFR-09's "any spin result can be forced in a test."
- **Statistical test:** AC-34 is inherently probabilistic; pick a sample size and tolerance that passes reliably (document the choice in the test, don't chase perfect fairness).
- **Performance test:** AC-35 asserts a wall-clock bound; run it with `-Dtest=SpinProfileTest#largeSpinEvaluatesUnder100ms` in isolation if CI noise makes it flaky, and say so if it does.

## 6. Definition of Done

- [ ] All AC-02 through AC-27, AC-34, AC-35 listed in Issue 2 pass as unit tests.
- [ ] `./mvnw verify` is green (format check + all tests, including Issue 1's Testcontainers test, which is untouched by this issue).
- [ ] No class under `domain` imports anything from `org.springframework.*` or `jakarta.persistence.*` (manual check, per CLAUDE.md's open point on NFR-10).
- [ ] A new prize rule can be added as one new class plus one registration line in `PrizeEvaluator`, with no edits to existing rule classes or their tests (NFR-11, demonstrated by the rule list design, not necessarily by actually adding a throwaway rule).
- [ ] The assumptions in section 3 have been confirmed or corrected.
- [ ] The diff has been reviewed (`/code-review` or a manual read of `git diff main...HEAD`).
- [ ] PR open with `Closes #2`, Issue 2's checkboxes ticked.

## 7. Open points for review

- `FixedSpinGenerator` ships in `src/main`; Issue 7 is responsible for making sure it (or any forced-spin wiring built on top of it) is never reachable in a production Spring profile.
- No upper-bound validation for slot/colour counts (still open — see Assumption 3 below, unchanged from the first draft).
