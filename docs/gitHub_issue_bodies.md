# Fruit Machine Service: full issue bodies

Seven issues. Each section below is a ready-to-paste GitHub issue body. The requirement text, decisions and acceptance criteria are written out so that a spec and plan can be produced from the issue alone. Full documents: docs/01 Business Requirements, 02 Use Cases, 03 Conceptual Design (keep them in the repo under /docs and reference them from CLAUDE.md).

Working rule for every issue: write the failing test for an acceptance criterion first, then the minimum code, then format, then commit.


---

# Issue 1: Project foundation

**Branch:** feature/1-project-foundation  |  **PR body:** Closes #1

## Goal
An empty but production-shaped project with tests, formatting, CI and Claude rules in place before any game code.

## Non-functional requirements
- **NFR-10 Maintainability:** The game rules can be understood and changed without touching web or storage code. Measure: An automated architecture check fails the build if rule code depends on web or storage code.
- **NFR-16 Portability:** A new team member can start the service and its data store with one command. Measure: One documented command starts both; tests run automatically on every change.

## Tasks (tick as you commit, test first)
- [ ] Spring Boot project, packages api / application / domain / infrastructure; JUnit 5, Mockito, Spring Boot Test, Testcontainers MySQL, Flyway, Spring Data JPA, validation, Actuator
- [ ] fmt-maven-plugin: format locally, check in CI
- [ ] GitHub Actions on every push and PR; main protected with required checks
- [ ] ArchUnit test: domain imports no Spring or JPA
- [ ] Pre-commit hook running the formatter
- [ ] CLAUDE.md: /init output + Karpathy guidelines + project rules (TDD, formatting, domain purity, Testcontainers, branch/PR naming, decisions D1-D11)

## Done when
A new clone builds, CI runs tests, a forbidden import or unformatted file fails the build.

---

# Issue 2: Domain core: the game engine

**Branch:** feature/2-domain-core-the-game-engine  |  **PR body:** Closes #2

## Goal
The complete game working in memory in pure Java: original rules, both float policies, driven by MachineConfig.

## Business requirements
- **BR-03 Spin generation** (Must). Each play produces one colour for every slot of the machine. Every colour must be equally likely in every slot, independently of the other slots.
- **BR-04 Prize evaluation** (Must). Every spin is classified into exactly one outcome: jackpot (all slots the same colour), full house (all slots different colours), small prize (a run of at least k adjacent slots of the same colour) or no win. When several apply, only the highest-priority prize counts: jackpot, then full house, then small prize.
- **BR-05 Prize payout** (Must). A jackpot pays the entire float. A full house pays half of the float, rounded down. A small prize pays five times the play cost. The machine never pays out more than the float it holds, and the float never becomes negative.
- **BR-06 Free plays** (Must). When a prize cannot be paid in full from the float, the player receives free plays instead: the unpaid amount divided by the play cost, rounded up. A free play costs nothing but can still win prizes. Free plays are kept on the machine as a balance. A jackpot never creates free plays.
- **BR-07 Float management** (Must). Each machine follows one float policy chosen at configuration. Under MIN_RESERVE (default) the machine refuses to start any play while its float is below five times the play cost, so every prize can always be paid in full. Under NONE the machine accepts every play and applies the shortfall rule of BR-06 literally.
- **BR-12 Variable slots and colours** (Must). The number of slots and the number of colours are settings, not fixed values. The rules of BR-04 apply to any number of slots (up to 10,000) and any number of colours (up to 1,000). A full house is impossible when there are more slots than colours.

## Use cases involved
- UC-02 Play a machine (paid play)
- UC-03 Play using a free-play credit
- UC-04 Evaluate a spin result
- UC-05 Settle the payout against the float
- UC-11 Reject: no free plays
- UC-10 Reject: machine needs refill
- UC-01 Configure a machine

## Decisions to respect
- **D1** Can one spin win several prizes? No. Only the single highest prize is paid: jackpot, then full house, then small prize.
- **D2** When does the play cost enter the float? Before evaluation: the stake joins the float, then the prize is paid.
- **D3** What are "free plays equal to the shortfall"? free plays = shortfall ÷ play cost, rounded up; the float is paid down to 0.
- **D4** How is money represented? Whole minor units (pence); no fractions.
- **D5** Half of an odd float? Rounded down; the remainder stays in the machine.
- **D6** Full house with more slots than colours? Impossible; the rule never matches.
- **D7** Does a free play pay the cost? No. The stake is not added, one credit is used, prizes are still paid.
- **D8** Where do free plays live? On the machine as a balance; there are no player accounts.
- **D9** Does a jackpot reset anything? The float becomes 0; the next paid play starts rebuilding it.
- **D10** What if the machine runs low on money? MIN_RESERVE: refuse plays while float < 5 × play cost; an operator refills.

## Non-functional requirements
- **NFR-01 Performance:** Evaluating a spin takes time proportional to the number of slots and does not depend on the number of colours. Measure: A 1,000,000-slot spin is evaluated in under 100 ms (AC-35).
- **NFR-03 Correctness:** Money is counted exactly and the float never goes below zero. Measure: 0 rounding differences in all money checks; float below 0 is never observed in any test.
- **NFR-09 Testability:** Any spin result can be forced in a test, and data-store behaviour is tested against a real database of the production type. Measure: Every prize in BR-04 has at least one forced-spin test; integration tests use a real database.
- **NFR-11 Maintainability:** A new prize rule can be added without editing existing rules. Measure: Adding a rule changes 1 new unit plus its registration; existing rule tests stay unchanged.
- **NFR-15 Correctness:** Each colour is equally likely in each slot. Measure: Over a large sample, observed frequencies are within a stated tolerance of equal (AC-34).

## Tasks (tick as you commit, test first)
- [ ] MachineConfig with validation and defaults
- [ ] SpinGenerator interface + secure-random implementation; fixed generator for tests
- [ ] One-pass SpinProfile (longest run, distinct colours)
- [ ] PrizeRule: JackpotRule, FullHouseRule, SmallPrizeRule; PrizeEvaluator (first match wins)
- [ ] PayoutSettler (paid = min(nominal, float); free plays = ceil(shortfall / cost); never for a jackpot)
- [ ] FloatPolicy: MinimumReservePolicy, NoReservePolicy; status ACTIVE / NEEDS_REFILL
- [ ] Machine aggregate: play(useFreePlay, generator), refill, status

## Acceptance criteria
- [ ] **AC-02 Defaults apply** (Positive)
  - Given an operator supplies only the play cost (100p) and the starting float (1,000p)
  - When the machine is configured
  - Then it has 4 slots, colours black/white/green/yellow, k = 2 and policy MIN_RESERVE
- [ ] **AC-03 Invalid settings are refused** (Negative)
  - Given an operator supplies k = 0, or k = 5 with 4 slots, or duplicate colours, or play cost 0, or a negative float
  - When the machine is configured
  - Then the request is refused with a list of the faulty fields and no machine is created
- [ ] **AC-04 Starting float must cover the reserve** (Boundary)
  - Given play cost 100p and policy MIN_RESERVE (reserve 500p)
  - When an operator configures starting float 499p, and then 500p
  - Then 499p is refused; 500p is accepted
- [ ] **AC-05 Large variable configuration** (Positive)
  - Given an operator supplies 10 slots, 300 colours, k = 3, play cost 50p and starting float 1,000p
  - When the machine is configured
  - Then the machine is created with exactly these settings
- [ ] **AC-06 Run length k** (Boundary)
  - Given a machine with 10 slots, 300 colours and k = 3
  - When a spin has a run of exactly 3 equal adjacent colours, and another spin has only runs of 2
  - Then the first wins a small prize; the second wins nothing
- [ ] **AC-07 Jackpot** (Positive)
  - Given the original game, float 1,000p and play cost 100p
  - When a paid play shows green, green, green, green
  - Then the float becomes 1,100p after the stake and the jackpot pays all 1,100p; the float is 0p
- [ ] **AC-08 Full house** (Positive)
  - Given the original game, float 1,000p and play cost 100p
  - When a paid play shows black, white, green, yellow
  - Then the float becomes 1,100p after the stake; a full house pays half, 550p; the float is 550p
- [ ] **AC-09 Small prize** (Positive)
  - Given the original game, float 1,000p and play cost 100p
  - When a paid play shows black, black, white, green
  - Then the float becomes 1,100p after the stake; the small prize pays 5 × 100p = 500p; the float is 600p
- [ ] **AC-10 No win** (Positive)
  - Given the original game, float 1,000p and play cost 100p
  - When a paid play shows black, white, black, white
  - Then nothing is paid out; the float is 1,100p
- [ ] **AC-11 Only the highest prize pays** (Positive)
  - Given a spin of four identical colours, which also contains runs of 2 or more
  - When the spin is evaluated
  - Then only the jackpot is paid; no small prize is added
- [ ] **AC-12 Half of an odd float is rounded down** (Boundary)
  - Given float 1,001p and play cost 100p
  - When a paid play wins a full house
  - Then the float becomes 1,101p, the prize is 550p (rounded down) and 551p stays in the machine
- [ ] **AC-13 Full house impossible with more slots than colours** (Boundary)
  - Given a machine with 5 slots and 4 colours
  - When any spin is evaluated
  - Then a full house is never awarded
- [ ] **AC-14 One-slot machine** (Boundary)
  - Given a machine with 1 slot
  - When any spin is evaluated
  - Then every spin is a jackpot
- [ ] **AC-15 Paid play result** (Positive)
  - Given an active machine
  - When a player makes a paid play
  - Then the response shows the slots, prize, amount paid, free plays earned, free-play balance, float after the play and status
- [ ] **AC-16 Refusal below the reserve** (Boundary)
  - Given a MIN_RESERVE machine with play cost 100p and float 499p
  - When a player tries to play
  - Then the play is refused (machine needs refill); the float, free-play balance and history are unchanged and no stake is taken
- [ ] **AC-17 Play at exactly the reserve** (Boundary)
  - Given a MIN_RESERVE machine with play cost 100p and float 500p
  - When a player plays and wins a small prize
  - Then the play is accepted; the float is 600p after the stake and the 500p prize is paid in full; no free plays are credited
- [ ] **AC-18 Status follows the float** (Positive)
  - Given a MIN_RESERVE machine that has just paid a jackpot, leaving float 0p
  - When the state is read, then 1,000p is refilled and the state is read again
  - Then status is NEEDS_REFILL, then ACTIVE
- [ ] **AC-22 Shortfall becomes a free play (policy NONE)** (Positive)
  - Given a NONE machine, play cost 100p, float 300p
  - When a paid play wins a small prize
  - Then the float becomes 400p after the stake, 400p is paid (500p due), the shortfall of 100p gives 1 free play and the float is 0p
- [ ] **AC-23 Shortfall is rounded up** (Boundary)
  - Given a NONE machine, play cost 100p, float 250p
  - When a paid play wins a small prize
  - Then the float becomes 350p, 350p is paid, the shortfall of 150p gives ceil(150 / 100) = 2 free plays
- [ ] **AC-24 A jackpot never creates free plays** (Boundary)
  - Given a NONE machine with float 0p and 1 free play
  - When the free play wins a jackpot
  - Then nothing is paid (the float is 0p) and no free plays are credited; the balance is 0
- [ ] **AC-25 Using a free-play credit** (Positive)
  - Given a NONE machine, play cost 100p, float 0p and 1 free play
  - When the player plays with the free play and wins a small prize
  - Then no stake is added; 0p is paid (500p due); the shortfall of 500p gives 5 free plays; the balance is 1 − 1 + 5 = 5
- [ ] **AC-26 Free play without a credit** (Negative)
  - Given a machine with free-play balance 0
  - When a player asks for a free play
  - Then the request is refused; no spin takes place
- [ ] **AC-27 Held free plays survive a refusal** (Boundary)
  - Given a MIN_RESERVE machine holding 1 free play with float below the reserve
  - When the player tries a free play, then the operator refills, then the player tries again
  - Then the first attempt is refused with the credit kept; after the refill the free play is accepted
- [ ] **AC-34 Fair spins** (Positive)
  - Given a machine with 4 slots and 4 colours
  - When a large number of spins is generated
  - Then each spin has exactly one colour per slot and each colour appears with roughly equal frequency in every slot
- [ ] **AC-35 Large spins are evaluated quickly** (Boundary)
  - Given a spin of 1,000,000 slots
  - When it is evaluated
  - Then evaluation completes in under 100 ms

## Done when
All listed AC pass as unit tests with no database or Spring.

---

# Issue 3: Persistence and application service

**Branch:** feature/3-persistence-and-application-service  |  **PR body:** Closes #3

## Goal
Durable state and one atomic, race-free transaction per play on MySQL.

## Business requirements
- **BR-02 Paid game play** (Must). A player can play a machine by paying the fixed play cost. The cost joins the float before the result is evaluated, and the player is told the spin, the prize, the amount paid, any free plays earned and the new float.
- **BR-08 Float refill** (Must). An operator can add money to a machine float. Each refill is recorded. A machine that was refusing plays because of a low float accepts plays again as soon as its float reaches the reserve.
- **BR-10 Play history and audit** (Must). Every completed play and every refill is recorded permanently, so that any play can be reconstructed afterwards: slots shown, prize, amount that should have been paid, amount actually paid, free plays earned and the float afterwards. A refused or failed request leaves no record.
- **BR-11 Multiple-machine support** (Must). The service hosts any number of machines at the same time. Each machine has its own configuration, float, free-play balance and history; activity on one machine never changes another.

## Use cases involved
- UC-02 Play a machine (paid play)
- UC-07 Reject invalid input
- UC-09 Reject: machine not found
- UC-12 Reject: machine busy (lock timeout)
- UC-08 Refill the float
- UC-03 Play using a free-play credit
- UC-01 Configure a machine

## Decisions to respect
- **D4** How is money represented? Whole minor units (pence); no fractions.
- **D11** How does a drained machine come back? An operator refill, recorded in its own audit history.

## Non-functional requirements
- **NFR-03 Correctness:** Money is counted exactly and the float never goes below zero. Measure: 0 rounding differences in all money checks; float below 0 is never observed in any test.
- **NFR-04 Concurrency:** Two simultaneous plays on one machine never spend the same money twice; different machines do not hold each other up. Measure: With 200 parallel plays on one machine, money conservation holds exactly (AC-33).
- **NFR-05 Consistency:** A play is all-or-nothing: stake, spin, payout, free plays and history record are applied together or not at all. Measure: After any failed request, float, free-play balance and history are identical to before it.
- **NFR-06 Reliability:** Machine state and history survive restarts and redeployments. Measure: After a restart, 100% of machines, floats, balances and records are still present.
- **NFR-07 Reliability:** An invalid state (negative float, impossible k, unknown prize) cannot be stored even if a programming error occurs. Measure: Attempts to store such a state are rejected by the data store in 100% of tests.
- **NFR-08 Auditability:** Every play can be reconstructed afterwards. Measure: 1 history record per completed play and 1 per refill; 0 for refused requests (AC-30).

## Tasks (tick as you commit, test first)
- [ ] Flyway V1: machine, play, refill; BIGINT pence, JSON columns, CHECK constraints, foreign keys, version column
- [ ] JPA entities + MachineMapper
- [ ] JpaMachineRepository: findForUpdate (about 2 s timeout), save, recordPlay, recordRefill
- [ ] MachineService.play: one transaction, lock, play, save, record, commit; rollback on any exception
- [ ] MachineService configure, read state, refill
- [ ] Play and refill history in the same transaction

## Acceptance criteria
- [ ] **AC-01 Create a machine** (Positive)
  - Given an operator supplies 4 slots, colours black/white/green/yellow, k = 2, play cost 100p, starting float 1,000p and policy MIN_RESERVE
  - When the machine is configured
  - Then a machine with a new unique id is created with exactly these settings and status ACTIVE (1,000p is above the 500p reserve)
- [ ] **AC-19 Refill** (Positive)
  - Given a machine with float 0p
  - When an operator refills 5,000p
  - Then the float is 5,000p, a refill record is stored and the machine is ACTIVE
- [ ] **AC-28 Read machine state** (Positive)
  - Given an existing machine
  - When an operator reads its state
  - Then configuration, float, reserve, free-play balance, total plays, total paid out and status are returned
- [ ] **AC-29 Unknown machine** (Negative)
  - Given a machine id that does not exist
  - When anyone plays it or reads it
  - Then the request is refused as not found and nothing changes
- [ ] **AC-30 Every play and refill is recorded** (Positive)
  - Given a machine on which 3 plays, 1 refused play and 1 refill are made
  - When the records are inspected
  - Then there are exactly 3 play records and 1 refill record, each with the fields of BR-10; the refused play left no record
- [ ] **AC-31 Machines are independent** (Positive)
  - Given two machines A and B with different settings
  - When plays and refills are made on A
  - Then B keeps its float, free-play balance and history unchanged

## Done when
A machine can be created, played and refilled through the service on real MySQL and state survives a restart.

---

# Issue 4: REST API

**Branch:** feature/4-rest-api  |  **PR body:** Closes #4

## Goal
Expose the use cases as a JSON API with consistent errors. API-only.

## Business requirements
- **BR-01 Machine configuration** (Must). An operator can create a fruit machine by choosing its number of slots, its colours, the run length k that wins a small prize, the cost of one play, the starting float and the float policy. Omitted choices fall back to the original game (4 slots; black, white, green, yellow; k = 2; policy MIN_RESERVE).
- **BR-02 Paid game play** (Must). A player can play a machine by paying the fixed play cost. The cost joins the float before the result is evaluated, and the player is told the spin, the prize, the amount paid, any free plays earned and the new float.
- **BR-09 Machine state** (Must). An operator can read the current state of a machine: its configuration, float, reserve, free-play balance, total plays, total paid out and its status. Status is ACTIVE when the float is at or above the reserve and NEEDS_REFILL otherwise (always ACTIVE under NONE).

## Use cases involved
- UC-01 Configure a machine
- UC-07 Reject invalid input
- UC-02 Play a machine (paid play)
- UC-09 Reject: machine not found
- UC-12 Reject: machine busy (lock timeout)
- UC-06 Read machine state
- UC-10 Reject: machine needs refill

## Non-functional requirements
- **NFR-12 Usability of the interface:** Responses are consistent, and every refusal states its reason in a standard error format. Measure: 100% of refusals carry a machine-readable reason and the right status category (400, 404, 409, 503).
- **NFR-13 Correctness:** Bad input is rejected before it can affect any machine. Measure: 0 invalid requests change state (AC-03, AC-20).

## Tasks (tick as you commit, test first)
- [ ] API-01 POST /api/v1/machines (201 + Location)
- [ ] API-02 POST /api/v1/machines/{id}/plays, body {"useFreePlay": false|true}; one request = one play
- [ ] API-03 GET /api/v1/machines/{id}
- [ ] API-04 POST /api/v1/machines/{id}/refills, body {"amountPence": n}
- [ ] Problem-details errors: 400 validation, 404 not found, 409 machine-needs-refill (with float and reserve), 409 no-free-plays, 503 + Retry-After, 500 without stack trace
- [ ] OpenAPI / Swagger UI with examples
- [ ] (optional) API-05 play history, API-06 machine list

## Acceptance criteria
- [ ] **AC-01 Create a machine** (Positive)
  - Given an operator supplies 4 slots, colours black/white/green/yellow, k = 2, play cost 100p, starting float 1,000p and policy MIN_RESERVE
  - When the machine is configured
  - Then a machine with a new unique id is created with exactly these settings and status ACTIVE (1,000p is above the 500p reserve)
- [ ] **AC-02 Defaults apply** (Positive)
  - Given an operator supplies only the play cost (100p) and the starting float (1,000p)
  - When the machine is configured
  - Then it has 4 slots, colours black/white/green/yellow, k = 2 and policy MIN_RESERVE
- [ ] **AC-03 Invalid settings are refused** (Negative)
  - Given an operator supplies k = 0, or k = 5 with 4 slots, or duplicate colours, or play cost 0, or a negative float
  - When the machine is configured
  - Then the request is refused with a list of the faulty fields and no machine is created
- [ ] **AC-15 Paid play result** (Positive)
  - Given an active machine
  - When a player makes a paid play
  - Then the response shows the slots, prize, amount paid, free plays earned, free-play balance, float after the play and status
- [ ] **AC-16 Refusal below the reserve** (Boundary)
  - Given a MIN_RESERVE machine with play cost 100p and float 499p
  - When a player tries to play
  - Then the play is refused (machine needs refill); the float, free-play balance and history are unchanged and no stake is taken
- [ ] **AC-19 Refill** (Positive)
  - Given a machine with float 0p
  - When an operator refills 5,000p
  - Then the float is 5,000p, a refill record is stored and the machine is ACTIVE
- [ ] **AC-20 Refill amount must be positive** (Negative)
  - Given any machine
  - When an operator refills 0p or a negative amount
  - Then the request is refused and nothing changes
- [ ] **AC-21 Refill of an unknown machine** (Negative)
  - Given a machine id that does not exist
  - When an operator refills it
  - Then the request is refused as not found
- [ ] **AC-25 Using a free-play credit** (Positive)
  - Given a NONE machine, play cost 100p, float 0p and 1 free play
  - When the player plays with the free play and wins a small prize
  - Then no stake is added; 0p is paid (500p due); the shortfall of 500p gives 5 free plays; the balance is 1 − 1 + 5 = 5
- [ ] **AC-26 Free play without a credit** (Negative)
  - Given a machine with free-play balance 0
  - When a player asks for a free play
  - Then the request is refused; no spin takes place
- [ ] **AC-28 Read machine state** (Positive)
  - Given an existing machine
  - When an operator reads its state
  - Then configuration, float, reserve, free-play balance, total plays, total paid out and status are returned
- [ ] **AC-29 Unknown machine** (Negative)
  - Given a machine id that does not exist
  - When anyone plays it or reads it
  - Then the request is refused as not found and nothing changes
- [ ] **AC-36 Malformed play request** (Negative)
  - Given any machine
  - When a play request is sent that cannot be understood (for example unreadable content or a free-play flag that is not true or false)
  - Then the request is refused with a list of the faults; the float, free-play balance and history are unchanged

## Done when
The configure, play, refused, refill flow works from Postman or Swagger and every refusal leaves state unchanged.

---

# Issue 5: Hardening and quality

**Branch:** feature/5-hardening-and-quality  |  **PR body:** Closes #5

## Goal
Prove correctness on a real MySQL and under concurrency.

## Business requirements
- **BR-02 Paid game play** (Must). A player can play a machine by paying the fixed play cost. The cost joins the float before the result is evaluated, and the player is told the spin, the prize, the amount paid, any free plays earned and the new float.
- **BR-10 Play history and audit** (Must). Every completed play and every refill is recorded permanently, so that any play can be reconstructed afterwards: slots shown, prize, amount that should have been paid, amount actually paid, free plays earned and the float afterwards. A refused or failed request leaves no record.
- **BR-11 Multiple-machine support** (Must). The service hosts any number of machines at the same time. Each machine has its own configuration, float, free-play balance and history; activity on one machine never changes another.

## Use cases involved
- UC-02 Play a machine (paid play)
- UC-07 Reject invalid input
- UC-09 Reject: machine not found
- UC-12 Reject: machine busy (lock timeout)
- UC-03 Play using a free-play credit
- UC-08 Refill the float
- UC-01 Configure a machine

## Non-functional requirements
- **NFR-04 Concurrency:** Two simultaneous plays on one machine never spend the same money twice; different machines do not hold each other up. Measure: With 200 parallel plays on one machine, money conservation holds exactly (AC-33).
- **NFR-05 Consistency:** A play is all-or-nothing: stake, spin, payout, free plays and history record are applied together or not at all. Measure: After any failed request, float, free-play balance and history are identical to before it.
- **NFR-07 Reliability:** An invalid state (negative float, impossible k, unknown prize) cannot be stored even if a programming error occurs. Measure: Attempts to store such a state are rejected by the data store in 100% of tests.
- **NFR-09 Testability:** Any spin result can be forced in a test, and data-store behaviour is tested against a real database of the production type. Measure: Every prize in BR-04 has at least one forced-spin test; integration tests use a real database.

## Tasks (tick as you commit, test first)
- [ ] Repository tests: migration, CHECK constraints, FOR UPDATE query
- [ ] End-to-end flow: configure, play, drain with a jackpot, refused, refill, play
- [ ] 200 parallel plays on one machine with a float large enough that none is refused
- [ ] (optional) property-based tests

## Acceptance criteria
- [ ] **AC-18 Status follows the float** (Positive)
  - Given a MIN_RESERVE machine that has just paid a jackpot, leaving float 0p
  - When the state is read, then 1,000p is refilled and the state is read again
  - Then status is NEEDS_REFILL, then ACTIVE
- [ ] **AC-19 Refill** (Positive)
  - Given a machine with float 0p
  - When an operator refills 5,000p
  - Then the float is 5,000p, a refill record is stored and the machine is ACTIVE
- [ ] **AC-31 Machines are independent** (Positive)
  - Given two machines A and B with different settings
  - When plays and refills are made on A
  - Then B keeps its float, free-play balance and history unchanged
- [ ] **AC-32 Two simultaneous plays** (Concurrency)
  - Given a MIN_RESERVE machine with float 500p and play cost 100p
  - When two players play at the same moment
  - Then the plays are processed one after the other; the second sees the float left by the first; no money is counted twice; if the machine cannot be reserved within about 2 seconds the request is refused as busy and nothing changes
- [ ] **AC-33 200 parallel plays keep money consistent** (Concurrency)
  - Given a machine with a float large enough that no play is refused
  - When 200 paid plays are made in parallel
  - Then float + total paid out = starting float + 200 × play cost, and exactly 200 play records exist

## Done when
All pass in CI.

---

# Issue 6: Delivery and documentation

**Branch:** feature/6-delivery-and-documentation  |  **PR body:** Closes #6

## Goal
Anyone can run, demo and understand the project in minutes.

## Non-functional requirements
- **NFR-14 Observability:** Every play is traceable, and play, payout and free-play counts can be measured. Measure: 1 log entry per play; counters for plays, payouts and credited free plays are available.
- **NFR-16 Portability:** A new team member can start the service and its data store with one command. Measure: One documented command starts both; tests run automatically on every change.
- **NFR-17 Security:** Inputs are bounded and no secrets are stored with the source code. Measure: Requests above the limits of NFR-02 are refused; 0 credentials found in the repository.

## Tasks (tick as you commit, test first)
- [ ] Docker Compose: application + MySQL 8; credentials from environment variables; least-privilege DB user
- [ ] Postman collection using a one-slot machine (certain jackpot) to show configure, play, refused, refill
- [ ] README: how to run, API summary, why MIN_RESERVE and refill exist (D10, D11), how policy NONE keeps the original rules
- [ ] (optional) k6 or Gatling script

## Done when
docker compose up plus the Postman collection demonstrates the whole product.

---

# Issue 7: System and acceptance verification

**Branch:** feature/7-verification  |  **PR body:** Closes #7

## Goal
Verify the finished, running product against the business scenarios (SC-01 to SC-13) and all 36 acceptance criteria, and record the evidence.

## Scope
- **System test (automated, black box):** start the whole application on a random port with a real MySQL (Testcontainers), call the API over HTTP, and swap in a scripted `SpinGenerator` in a **test-only** configuration so every prize can be forced. This configuration must never be active in production.
- **User acceptance test (UAT):** run the scenarios through Postman or Swagger against `docker compose up`. Spins are random (review note RN-11), so use machines with a certain outcome (one slot or one colour always gives a jackpot) and check that each response is consistent with the rules (payout matches the prize, float arithmetic adds up).

## Entry criteria
Issues 1 to 6 merged, CI green, `docker compose up` works.

## Tasks (tick as you commit, test first)
- [ ] System tests for SC-01 to SC-12 with a scripted spin generator over real HTTP and real MySQL
- [ ] System test for SC-13: two simultaneous plays, then 200 parallel plays (AC-32, AC-33)
- [ ] Postman collection for UAT (configure, play, refused, refill, invalid input, unknown machine) stored in the repo, runnable with Newman
- [ ] `verification.md`: table with columns AC or scenario, how verified (system test / UAT), result, date; pre-filled with AC-01 to AC-36 and SC-01 to SC-13
- [ ] Update the master traceability matrix: mark each requirement verified or explain why not
- [ ] Review notes RN-01 to RN-11 re-read; anything still open is listed in `verification.md`

## How each scenario is verified
| Scenario | System test (scripted spin) | UAT (Postman, real service) |
|---|---|---|
| SC-01, SC-02 configure | Exact | Exact |
| SC-03 to SC-06 prizes | Exact, forced spins | Response-checking; jackpot via a one-slot machine |
| SC-07 to SC-09 free plays (policy NONE) | Exact | Response-checking |
| SC-10 refusal, SC-11 refill | Exact | Exact (one-slot jackpot drains the machine) |
| SC-12 invalid input, unknown machine | Exact | Exact |
| SC-13 two simultaneous plays | 200 parallel plays | Optional (Postman Runner or k6) |

## Exit criteria
- All 36 acceptance criteria passed, or each failure explained and accepted
- All 13 business scenarios passed
- No open defect of blocking severity
- Postman collection and `verification.md` committed

## Done when
Every acceptance criterion has a recorded result and the traceability matrix shows requirement, test and result.
