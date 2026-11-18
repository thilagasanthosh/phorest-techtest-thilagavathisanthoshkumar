
# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.



## Project status

Fruit Machine Service (Phorest tech test): a Spring Boot REST API that hosts configurable fruit machines (slots, colours, float, free plays). Work is staged as 7 GitHub issues, each on its own branch (`feature/<n>-<slug>`, PR body `Closes #<n>`). Currently only the Issue 1 foundation exists (Spring Boot scaffold, layer packages, formatter, CI, pre-commit hook); there is no game code yet. The plan lives in:

- `docs/gitHub_issue_bodies.md`: full issue bodies with requirements (BR-xx), NFRs, decisions (D1-D11) and acceptance criteria (AC-01..36). Read the relevant issue before implementing anything.
- `docs/01-business-requirements.docx`, `02-use-cases.docx`, `03-conceptual-design.docx`: source design documents.
- `docs/verification.md`: empty; to be filled in Issue 7 (AC/scenario, how verified, result, date).

NFR-16 ("one command starts service and data store") is only partially met after Issue 1: tests run automatically via Testcontainers, but the one-command Docker Compose startup lands in Issue 6.

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

<!-- cce-block-version: 4 -->
## Context Engine (CCE)

This project uses Code Context Engine for intelligent code retrieval and
cross-session memory.

### Searching the codebase

**You MUST use `context_search` instead of reading files directly** when
exploring the codebase, answering questions about code, or understanding how
things work. This is a hard requirement, not a suggestion. `context_search`
returns the most relevant code chunks with confidence scores instead of whole
files, and tracks token savings automatically.

When to use `context_search`:
- Answering questions about the codebase ("how does X work?", "where is Y?")
- Exploring structure or architecture
- Finding related code, functions, or patterns
- Any time you would otherwise read a file just to understand it

When to use `Read` instead:
- You need to edit a specific file (read before editing)
- You need the exact, complete content of a known file path

Other search tools:
- `expand_chunk` — get full source for a compressed result
- `related_context` — find what calls/imports a function

### Cross-session memory — use it actively

This project has persistent memory across Claude Code sessions. **You must
use it both ways: recall before answering, record after deciding.** Memory
that is not recorded is lost; memory that is not recalled does nothing.

**Before answering a non-trivial question, call `session_recall`.**
Especially when:
- The question touches architecture, design, or naming choices
- The user asks "what / why / how did we ..."
- You are about to recommend an approach the team may have already chosen
  or already rejected

Pass a topic phrase, not a single word — e.g. `session_recall("auth flow")`,
not `session_recall("auth")`. Recall is vector-similarity-based, so paraphrases
match. If recall returns relevant entries, lead with them ("Per a prior
decision: ...") instead of re-deriving the answer.

**After making a non-obvious decision, call `record_decision`.** Especially:
- Choosing one library / pattern / approach over another
- Resolving an ambiguity in the spec or requirements
- Establishing a convention the project should follow going forward
- Anything you would not want to re-litigate next session

Format: `record_decision(decision="...", reason="...")`. Keep both fields
short and specific — they are surfaced verbatim at the start of future
sessions.

**After meaningful work in a file, call `record_code_area`.** Especially when:
- You added or substantially modified a function/class
- You traced through a non-obvious flow and want future-you to find it fast

Format: `record_code_area(file_path="...", description="...")`.

Skip recording for trivial reads, formatting changes, or one-off lookups —
the goal is durable signal, not an event log.

### Drilling deeper from a recall hit

`session_recall` results are tagged with the source session id, e.g.
`[turn sid:abc123|n:5]`. To drill in:

- `session_timeline(session_id="abc123")` — walk the per-turn summaries of
  that session in order. Use this when the user asks "what was the
  reasoning?" or "how did we get there?".
- `session_event(event_id=N)` — fetch a specific tool event's raw input
  and output (capped at 4 KB at read time). Use this when a turn summary
  references a tool result you actually need to inspect.

Both are read-only and cheap. Prefer them over re-running tool calls or
asking the user to re-paste context.

### Output style

Respond in compressed style. Drop articles (a, an, the) in prose. Use
sentence fragments over full sentences. Use short synonyms (fix not resolve,
check not investigate). Pattern: [thing] [action] [reason]. [next step].
No filler, hedging, pleasantries, trailing summaries, or restating what
the user said. One sentence if one sentence is enough.

When suggesting code changes, show only the changed lines with 3 lines of
context. Never rewrite entire files. Multiple changes in one file: show each
change separately. Never echo back unchanged code the user already has.

Code blocks, file paths, commands, error messages: always written in full.
Security warnings and destructive action confirmations: use full clarity.
<!-- /cce-block -->
Be concise with all of your responses