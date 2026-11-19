# Verification

Evidence for the acceptance criteria (AC-01..36, from `docs/gitHub_issue_bodies.md`) and business scenarios (SC-01..13, from `docs/02-use-cases.docx`).

**Result values:** `Not run` · `Pass` · `Fail` (explain in the Result column and in the Notes section) · `Accepted gap` (explain why).
**Evidence:** relative links to files in `docs/verification/evidence/`. Long output goes in a file; the cell holds only the link.

## Scenarios

| Scenario | Title | How verified | Result | Date | Evidence |
|---|---|---|---|---|---|
| SC-01 | Original 4-slot configuration | System test (exact); UAT (exact) | Not run | | |
| SC-02 | Variable configuration | System test (exact); UAT (exact) | Not run | | |
| SC-03 | No prize | System test (forced spin); UAT (response check) | Not run | | |
| SC-04 | Jackpot | System test (forced spin); UAT (one-slot machine) | Not run | | |
| SC-05 | Full house | System test (forced spin); UAT (response check) | Not run | | |
| SC-06 | Small prize | System test (forced spin); UAT (response check) | Not run | | |
| SC-07 | Shortfall under policy NONE | System test (exact); UAT (response check) | Not run | | |
| SC-08 | Free plays credited, rounded up | System test (exact); UAT (response check) | Not run | | |
| SC-09 | Using a free-play credit | System test (exact); UAT (response check) | Not run | | |
| SC-10 | Refusal under MIN_RESERVE | System test (exact); UAT (exact) | Not run | | |
| SC-11 | Operator refill | System test (exact); UAT (exact) | Not run | | |
| SC-12 | Invalid input / unknown machine | System test (exact); UAT (exact) | Not run | | |
| SC-13 | Two simultaneous plays | System test (200 parallel plays); UAT (optional) | Not run | | |

## Acceptance criteria

| AC | Title | Type | How verified | Result | Date | Evidence |
|---|---|---|---|---|---|---|
| AC-01 | Create a machine | Positive | | Not run | | |
| AC-02 | Defaults apply | Positive | | Not run | | |
| AC-03 | Invalid settings are refused | Negative | | Not run | | |
| AC-04 | Starting float must cover the reserve | Boundary | | Not run | | |
| AC-05 | Large variable configuration | Positive | | Not run | | |
| AC-06 | Run length k | Boundary | | Not run | | |
| AC-07 | Jackpot | Positive | | Not run | | |
| AC-08 | Full house | Positive | | Not run | | |
| AC-09 | Small prize | Positive | | Not run | | |
| AC-10 | No win | Positive | | Not run | | |
| AC-11 | Only the highest prize pays | Positive | | Not run | | |
| AC-12 | Half of an odd float is rounded down | Boundary | | Not run | | |
| AC-13 | Full house impossible with more slots than colours | Boundary | | Not run | | |
| AC-14 | One-slot machine | Boundary | | Not run | | |
| AC-15 | Paid play result | Positive | | Not run | | |
| AC-16 | Refusal below the reserve | Boundary | | Not run | | |
| AC-17 | Play at exactly the reserve | Boundary | | Not run | | |
| AC-18 | Status follows the float | Positive | | Not run | | |
| AC-19 | Refill | Positive | | Not run | | |
| AC-20 | Refill amount must be positive | Negative | | Not run | | |
| AC-21 | Refill of an unknown machine | Negative | | Not run | | |
| AC-22 | Shortfall becomes a free play (policy NONE) | Positive | | Not run | | |
| AC-23 | Shortfall is rounded up | Boundary | | Not run | | |
| AC-24 | A jackpot never creates free plays | Boundary | | Not run | | |
| AC-25 | Using a free-play credit | Positive | | Not run | | |
| AC-26 | Free play without a credit | Negative | | Not run | | |
| AC-27 | Held free plays survive a refusal | Boundary | | Not run | | |
| AC-28 | Read machine state | Positive | | Not run | | |
| AC-29 | Unknown machine | Negative | | Not run | | |
| AC-30 | Every play and refill is recorded | Positive | | Not run | | |
| AC-31 | Machines are independent | Positive | | Not run | | |
| AC-32 | Two simultaneous plays | Concurrency | | Not run | | |
| AC-33 | 200 parallel plays keep money consistent | Concurrency | | Not run | | |
| AC-34 | Fair spins | Positive | | Not run | | |
| AC-35 | Large spins are evaluated quickly | Boundary | | Not run | | |
| AC-36 | Malformed play request | Negative | | Not run | | |

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
| RN-11 | Not reviewed | Spins are random; UAT uses certain-outcome machines. |

## Open items

None recorded yet. List any open defect or accepted gap here, with the reason.
