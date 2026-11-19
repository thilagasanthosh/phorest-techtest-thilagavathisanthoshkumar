# Verification

Evidence for the acceptance criteria (AC-01..36, from `docs/gitHub_issue_bodies.md`) and business scenarios (SC-01..13, from `docs/02-use-cases.docx`).

**Result values:** `Pass` · `Fail` (explain in Open items) · `Not run` · `Open` (no test yet, reason in Open items).
**Methods:** `System` = failsafe `*IT` over real HTTP, real MySQL, scripted spins (`src/test/.../api/*SystemIT.java`). `UAT` = Postman collection `postman/fruitmachine-api.postman_collection.json` run with Newman against `docker compose` (82 assertions, 0 failed).
**Evidence:** `failsafe/` holds the failsafe report per class; `uat-run.json` is the Newman export. Both are in `docs/verification/evidence/`.

Run on 2026-11-19 with JDK 21 (`./mvnw -B verify`, BUILD SUCCESS, 17 system ITs) and Newman 6.2.3 against a fresh `docker compose -p fmuat up -d --build`.

## Scenarios

| Scenario | Title | Result | Method | Date | Evidence |
|---|---|---|---|---|---|
| SC-01 | Original 4-slot configuration | Pass | System (`ConfigureSystemIT.sc01…`); UAT (1.2) | 2026-11-19 | [failsafe](verification/evidence/failsafe/com.thilaga.fruitmachine.api.ConfigureSystemIT.txt), [newman](verification/evidence/uat-run.json) |
| SC-02 | Variable configuration (10 slots, 300 colours, k = 3) | Pass | System (`ConfigureSystemIT.sc02…`); UAT not run | 2026-11-19 | [failsafe](verification/evidence/failsafe/com.thilaga.fruitmachine.api.ConfigureSystemIT.txt) |
| SC-03 | No prize | Pass | System (`PrizeSystemIT.sc03…`) | 2026-11-19 | [failsafe](verification/evidence/failsafe/com.thilaga.fruitmachine.api.PrizeSystemIT.txt) |
| SC-04 | Jackpot | Pass | System (`PrizeSystemIT.sc04…`); UAT (3.5, one-slot) | 2026-11-19 | [failsafe](verification/evidence/failsafe/com.thilaga.fruitmachine.api.PrizeSystemIT.txt), [newman](verification/evidence/uat-run.json) |
| SC-05 | Full house, odd float | Pass | System (`PrizeSystemIT.sc05…`) | 2026-11-19 | [failsafe](verification/evidence/failsafe/com.thilaga.fruitmachine.api.PrizeSystemIT.txt) |
| SC-06 | Small prize | Pass | System (`PrizeSystemIT.sc06…`) | 2026-11-19 | [failsafe](verification/evidence/failsafe/com.thilaga.fruitmachine.api.PrizeSystemIT.txt) |
| SC-07 | Shortfall under policy NONE | Pass | System (`FreePlaySystemIT.sc07…`) | 2026-11-19 | [failsafe](verification/evidence/failsafe/com.thilaga.fruitmachine.api.FreePlaySystemIT.txt) |
| SC-08 | Free plays credited, rounded up | Pass | System (`FreePlaySystemIT.sc08…`) | 2026-11-19 | [failsafe](verification/evidence/failsafe/com.thilaga.fruitmachine.api.FreePlaySystemIT.txt) |
| SC-09 | Using a free-play credit; zero balance refused | Pass | System (`FreePlaySystemIT.sc09…`) | 2026-11-19 | [failsafe](verification/evidence/failsafe/com.thilaga.fruitmachine.api.FreePlaySystemIT.txt) |
| SC-10 | Refusal under MIN_RESERVE | Pass | System (`RefusalSystemIT.sc10…`); UAT (3.6, 3.7) | 2026-11-19 | [failsafe](verification/evidence/failsafe/com.thilaga.fruitmachine.api.RefusalSystemIT.txt), [newman](verification/evidence/uat-run.json) |
| SC-11 | Operator refill | Pass | System (`RefusalSystemIT.sc11…`); UAT (4.2, 3.8) | 2026-11-19 | [failsafe](verification/evidence/failsafe/com.thilaga.fruitmachine.api.RefusalSystemIT.txt), [newman](verification/evidence/uat-run.json) |
| SC-12 | Invalid input / unknown machine | Pass | System (`ConfigureSystemIT.sc12…`); UAT (1.4, 2.2, 3.14) | 2026-11-19 | [failsafe](verification/evidence/failsafe/com.thilaga.fruitmachine.api.ConfigureSystemIT.txt), [newman](verification/evidence/uat-run.json) |
| SC-13 | Two simultaneous plays; 200 parallel plays | Pass | System (`ConcurrencySystemIT`); UAT optional, not run | 2026-11-19 | [failsafe](verification/evidence/failsafe/com.thilaga.fruitmachine.api.ConcurrencySystemIT.txt) |

## Acceptance criteria (traceability: requirement → test → result)

| AC | Title | Result | Test / method | Date | Evidence |
|---|---|---|---|---|---|
| AC-01 | Create a machine | Pass | System `sc01`; UAT 1.2 | 2026-11-19 | failsafe, newman |
| AC-02 | Defaults apply | Pass | UAT 1.1 | 2026-11-19 | newman |
| AC-03 | Invalid settings are refused | Pass | System `sc12` (k > slots); UAT 1.3–1.7 | 2026-11-19 | failsafe, newman |
| AC-04 | Starting float must cover the reserve | Pass | UAT 1.8, 1.9 | 2026-11-19 | newman |
| AC-05 | Large variable configuration | Pass | System `sc02`; UAT 1.10 | 2026-11-19 | failsafe, newman |
| AC-06 | Run length k | Pass | System `sc02` (run of 3 wins, run of 2 does not) | 2026-11-19 | failsafe |
| AC-07 | Jackpot | Pass | System `sc04`; UAT 3.5 | 2026-11-19 | failsafe, newman |
| AC-08 | Full house | Pass | System `sc05` | 2026-11-19 | failsafe |
| AC-09 | Small prize | Pass | System `sc06` | 2026-11-19 | failsafe |
| AC-10 | No win | Pass | System `sc03` | 2026-11-19 | failsafe |
| AC-11 | Only the highest prize pays | Open | Not asserted by any system or UAT test (see Open items) | | |
| AC-12 | Half of an odd float is rounded down | Pass | System `sc05` (1001p → 550 paid, 551 left) | 2026-11-19 | failsafe |
| AC-13 | Full house impossible with more slots than colours | Open | No test (see Open items) | | |
| AC-14 | One-slot machine | Pass | UAT 3.4 | 2026-11-19 | newman |
| AC-15 | Paid play result | Pass | System `sc06` (prize, nominal, paid, float); UAT 3.1 | 2026-11-19 | failsafe, newman |
| AC-16 | Refusal below the reserve | Pass | System `sc10`; UAT 3.6, 3.7 | 2026-11-19 | failsafe, newman |
| AC-17 | Play at exactly the reserve | Pass | System `sc10` (500p accepted); UAT 3.3 | 2026-11-19 | failsafe, newman |
| AC-18 | Status follows the float | Pass | System `sc04`, `sc11`; UAT 3.5, 3.8 | 2026-11-19 | failsafe, newman |
| AC-19 | Refill | Pass | System `sc11`; UAT 4.2 | 2026-11-19 | failsafe, newman |
| AC-20 | Refill amount must be positive | Pass | UAT 4.3, 4.4, 4.5 | 2026-11-19 | newman |
| AC-21 | Refill of an unknown machine | Pass | UAT 4.6 | 2026-11-19 | newman |
| AC-22 | Shortfall becomes a free play (policy NONE) | Pass | System `sc07` | 2026-11-19 | failsafe |
| AC-23 | Shortfall is rounded up | Pass | System `sc08` | 2026-11-19 | failsafe |
| AC-24 | A jackpot never creates free plays | Open | Not asserted by any test (see Open items) | | |
| AC-25 | Using a free-play credit | Pass | System `sc09` | 2026-11-19 | failsafe |
| AC-26 | Free play without a credit | Pass | System `sc09` (zero balance → 409); UAT 3.10 | 2026-11-19 | failsafe, newman |
| AC-27 | Held free plays survive a refusal | Open | Not asserted by any test (see Open items) | | |
| AC-28 | Read machine state | Pass | System `sc04`, `sc11` (GET); UAT 2.1 | 2026-11-19 | failsafe, newman |
| AC-29 | Unknown machine | Pass | System `sc12`; UAT 2.2, 3.14 | 2026-11-19 | failsafe, newman |
| AC-30 | Every play and refill is recorded | Pass | System `sc11` (1 refill row), `sc13` (play rows); UAT 3.2 | 2026-11-19 | failsafe, newman |
| AC-31 | Machines are independent | Open | Not asserted by any test (see Open items) | | |
| AC-32 | Two simultaneous plays | Pass | System `sc13` (one 200 and one 409, final float 100) | 2026-11-19 | failsafe |
| AC-33 | 200 parallel plays keep money consistent | Pass | System `ac33_…` (accepted + busy = 200; float = 500 + 100 × accepted) | 2026-11-19 | failsafe |
| AC-34 | Fair spins | Open | Not verified (see Open items) | | |
| AC-35 | Large spins are evaluated quickly | Open | Not verified (see Open items) | | |
| AC-36 | Malformed play request | Pass | UAT 3.11, 3.12, 3.13 | 2026-11-19 | newman |

## Review notes (RN-01..RN-11)

| RN | Status | Notes |
|---|---|---|
| RN-01 | Not reviewed | |
| RN-02 | Not reviewed | |
| RN-03 | Not reviewed | |
| RN-04 | Not reviewed | |
| RN-05 | Not reviewed | |
| RN-06 | Not reviewed | |
| RN-07 | Not reviewed | |
| RN-08 | Not reviewed | |
| RN-09 | Not reviewed | |
| RN-10 | Not reviewed | |
| RN-11 | Addressed in part | Spins are random in production; system tests force spins via test-only `ScriptedSpins`; UAT checks by rule. |

## Open items

1. **AC-11, AC-13, AC-24, AC-27, AC-31:** no system or UAT test asserts them. Task 0 (audit of existing MockMvc ITs) not done, so they may be covered by existing `*Test` classes. Not checked.
2. **AC-34, AC-35:** the Issue 2 domain tests were not checked (plan Task 7). No result recorded.
3. **RN-01 to RN-10:** not re-read in this run.
4. **AC-33:** the busy (503) count is not recorded; the rule passed for any split.
5. **Pass rule is per-criterion, not per-field:** AC-15 and AC-30 are marked Pass from partial assertions (see the test names above).
6. **UAT for SC-02, SC-03, SC-05 to SC-09 and SC-13:** not run in Postman. Those scenarios are covered by system ITs only.
