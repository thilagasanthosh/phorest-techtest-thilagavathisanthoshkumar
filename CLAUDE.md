# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project status

Fruit Machine Service (Phorest tech test): a Spring Boot REST API that hosts configurable fruit machines (slots, colours, float, free plays). Work is staged as 7 GitHub issues, each on its own branch (`feature/<n>-<slug>`, PR body `Closes #<n>`). Currently only the Issue 1 foundation exists (Spring Boot scaffold, layer packages, formatter, CI, pre-commit hook); there is no game code yet. The plan lives in:

- `docs/gitHub_issue_bodies.md`: full issue bodies with requirements (BR-xx), NFRs, decisions (D1-D11) and acceptance criteria (AC-01..36). Read the relevant issue before implementing anything.
- `docs/01-business-requirements.docx`, `02-use-cases.docx`, `03-conceptual-design.docx`: source design documents.
- `docs/verification.md`: empty; to be filled in Issue 7 (AC/scenario, how verified, result, date).

## Commands

Java 21, Maven wrapper, Spring Boot 4.1.x. Tests use Testcontainers MySQL, so Docker must be running. Run Maven with a JDK 21: the bundled google-java-format crashes on newer JDKs (seen on JDK 27).

```
./mvnw verify                                   # build + all tests (includes format check)
./mvnw fmt:format                               # format source files locally
./mvnw test -Dtest=ClassName#methodName         # single test
./mvnw spring-boot:run                          # run app (needs a MySQL; or run src/test/.../TestFruitmachineApplication for a Testcontainers-backed dev instance)
```

`fmt-maven-plugin` (Spotify, 2.29) is configured: the `check` goal runs at the `validate` phase so `./mvnw verify` fails if any file is unformatted. Run `./mvnw fmt:format` to fix locally before committing.

One-time per clone, enable the pre-commit hook: `git config core.hooksPath .githooks`. It runs `fmt:format` on staged Java files and aborts the commit if anything was reformatted, so review, `git add`, and commit again.

CI (`.github/workflows/ci.yml`, job `build`) runs `./mvnw -B verify` on every push and PR with JDK 21. No secrets are needed.

## Architecture (target)

Packages under `com.thilaga.fruitmachine`: `api` / `application` / `domain` / `infrastructure`.

- `domain` must be pure Java: no Spring or JPA imports. This is not enforced by an automated check yet, so verify it in review. It holds the `Machine` aggregate, `MachineConfig`, `PrizeRule`s (Jackpot, FullHouse, SmallPrize; first match wins via `PrizeEvaluator`), `PayoutSettler`, `FloatPolicy` (MIN_RESERVE default, NONE), and a `SpinGenerator` interface (secure-random impl; scripted/fixed impl for tests).
- `application` has `MachineService`: one transaction per play (pessimistic lock `findForUpdate` ~2 s timeout, play, save, record history, rollback on any exception).
- `infrastructure` has JPA entities, mapper, repository, and Flyway migrations (BIGINT pence, JSON columns, CHECK constraints, version column).
- `api` has REST controllers under `/api/v1/machines` with problem-details errors (400/404/409/503).

## Workflow

Standard stages for every issue. Never skip a stage.

```
GitHub Issue → Feature Spec → Implementation Plan → Implementation → Tests → Review → PR
```

### Prompt: Create feature spec

```
Act as a Senior Java Solution Architect and Specification-Driven Development Lead.

Create a feature specification for Issue #N.

Read:
- docs/gitHub_issue_bodies.md  (find the Issue #N section)
- docs/01-business-requirements.docx
- docs/02-use-cases.docx
- docs/03-conceptual-design.docx
- docs/specs/  (existing specs for consistency)

Output: docs/specs/US-NN-<slug>.md

Include:
1. Story information (issue #, URL, story ID, title, parent epic, business objective)
2. Requirement traceability (BR-xx, UC-xx, AC-xx, architecture refs, API refs)
3. Functional specification (inputs/outputs, business rules, defaults, validation, main flow, alternative flows, exceptions)
4. Technical design boundaries (components, domain objects, persistence, API contract, transactions)
5. Acceptance scenarios (Given/When/Then — positive, negative, boundary)
6. Testing expectations (unit, integration, API/Postman)
7. Dependencies and out of scope
8. Definition of Done

Constraints:
- Do not invent requirements. Source docs are the truth.
- One Spring Boot app, pure Java domain, no microservices, no separate frontend.
- Keep scope small enough for one focused PR.
- Identify any ambiguity or conflicts between source docs and the issue.

Do not write implementation code or an implementation plan yet.
After the spec, give a one-paragraph summary: scope, dependencies, assumptions, unresolved questions.
```

### Prompt: Create implementation plan

```
Act as a Senior Java Solution Architect and TDD Lead.

Create an implementation plan for Issue #N.

Read:
- docs/gitHub_issue_bodies.md  (Issue #N section — requirements, decisions, AC)
- docs/specs/US-NN-<slug>.md   (the feature spec)
- src/  (existing source to understand current state)
- CLAUDE.md  (architecture, key rules, decisions D1-D11)

Output: docs/plans/US-NN-<slug>-plan.md

Include:
1. Prerequisites — which earlier issues must be merged before this one
2. Branch name  (feature/<n>-<slug>)
3. Ordered task list — for each task:
   a. Acceptance criterion it satisfies (AC-xx)
   b. Files to create or modify (package + class name)
   c. Test to write first (class name, method name, what it asserts)
   d. Minimal production code to make the test pass
   e. Verify step (command or check)
4. TDD cycle reminder: failing test → minimal code → ./mvnw fmt:format → commit
5. Test strategy (unit tests with no Spring/JPA, integration tests with Testcontainers MySQL, API tests via Postman/Swagger)
6. Definition of Done checklist aligned with the spec

Constraints:
- Follow architecture: domain = pure Java; application = service + transaction; infrastructure = JPA/Flyway; api = REST controllers.
- Money always as int pence. No floating point.
- Integration tests use real MySQL (Testcontainers), never H2 or in-memory.
- Do not add tasks beyond what the issue and spec require.
- Decisions D1-D11 are settled — do not revisit them.

Do not write any source code yet.
After the plan, list: estimated task count, risks, and anything that needs clarification before starting.
```

## Branches and PRs

Branch `feature/<n>-<slug>` per issue, PR body `Closes #<n>`. `main` is the target.

## Key rules

- TDD: write the failing test for an acceptance criterion first, then minimal code, then format, then commit.
- Money is whole minor units (pence) as integers; never floating point. Half of an odd float rounds down; free plays = ceil(shortfall / play cost); a jackpot never creates free plays.
- Only the single highest prize pays (jackpot > full house > small prize). Spin evaluation must be one pass, O(slots), independent of colour count (1,000,000 slots < 100 ms).
- New prize rules must be addable without editing existing rules.
- Integration tests use real MySQL via Testcontainers, never an in-memory substitute.
- Any forced-spin system-test configuration must be test-only and never active in production.
- Decisions D1-D11 are settled; don't re-litigate them (full text in the issue bodies):
  - D1 only the highest prize pays. D2 the stake joins the float before evaluation. D3 free plays = ceil(shortfall / play cost), float paid down to 0.
  - D4 money in whole pence. D5 half of an odd float rounds down. D6 full house is impossible when slots > colours.
  - D7 a free play adds no stake and uses one credit. D8 free plays are a balance on the machine, no player accounts. D9 a jackpot sets the float to 0.
  - D10 MIN_RESERVE refuses plays while float < 5 × play cost. D11 refills are operator actions with their own audit history.

---

## Behavioural guidelines (Karpathy)

Behavioral guidelines to reduce common LLM coding mistakes. These bias toward caution over speed. For trivial tasks, use judgment.

### 1. Think Before Coding

**Don't assume. Don't hide confusion. Surface tradeoffs.**

Before implementing:
- State your assumptions explicitly. If uncertain, ask.
- If multiple interpretations exist, present them — don't pick silently.
- If a simpler approach exists, say so. Push back when warranted.
- If something is unclear, stop. Name what's confusing. Ask.

### 2. Simplicity First

**Minimum code that solves the problem. Nothing speculative.**

- No features beyond what was asked.
- No abstractions for single-use code.
- No "flexibility" or "configurability" that wasn't requested.
- No error handling for impossible scenarios.
- If you write 200 lines and it could be 50, rewrite it.

Ask yourself: "Would a senior engineer say this is overcomplicated?" If yes, simplify.

### 3. Surgical Changes

**Touch only what you must. Clean up only your own mess.**

When editing existing code:
- Don't "improve" adjacent code, comments, or formatting.
- Don't refactor things that aren't broken.
- Match existing style, even if you'd do it differently.
- If you notice unrelated dead code, mention it — don't delete it.

When your changes create orphans:
- Remove imports/variables/functions that YOUR changes made unused.
- Don't remove pre-existing dead code unless asked.

The test: Every changed line should trace directly to the user's request.

### 4. Goal-Driven Execution

**Define success criteria. Loop until verified.**

Transform tasks into verifiable goals:
- "Add validation" → "Write tests for invalid inputs, then make them pass"
- "Fix the bug" → "Write a test that reproduces it, then make it pass"
- "Refactor X" → "Ensure tests pass before and after"

For multi-step tasks, state a brief plan:
```
1. [Step] → verify: [check]
2. [Step] → verify: [check]
3. [Step] → verify: [check]
```

Strong success criteria let you loop independently. Weak criteria ("make it work") require constant clarification.
