# cloud-itonami-isic-942: Trade Union Administrative Coordination Actor

ISIC 942 (Activities of trade unions) actor for the cloud-itonami fleet. Governs union-local administrative coordination operations: member enrollment logistics, event scheduling, dues-processing, supply requests, and staff shift coordination.

**Scope**: Administrative coordination only. Hard-coded blocks on collective bargaining, grievance adjudication, strike authorization, union leadership decisions, and disciplinary action.

## Quick Start

```bash
# Run comprehensive test suite (20 test cases)
nbb test_runner.kotoba

# Demo scenarios
kbb --backend sci -e "(require 'tradeunionorg.sim) (pprint (tradeunionorg.sim/run-scenarios))"
```

## Architecture

All modules are `.cljc` (portable Clojure/ClojureScript):

- **store.cljc** — MemStore protocol; member/event directory; demo data (3 members, 2 events, 3 accounts, append-only ledger)
- **advisor.cljc** — Proposal enrichment (deterministic demo); preserves original proposal fields and adds `:advisor-reasoning` + `:confidence`
- **governor.cljc** — Three HARD checks; no overrides; member verification (if member-id present)
- **operation.cljc** — StateGraph-style flow: intake → advise → govern → decide → commit | hold | escalate
- **phase.cljc** — Rollout phases 0–3 (which ops auto-commit, which escalate)
- **sim.cljc** — 5 demo scenarios (happy path, hard checks, escalation)
- **test.cljc** — 20 comprehensive test cases

## Operations (Closed Allowlist)

- **`:schedule-member-meeting`** — Meeting/event scheduling logistics
- **`:coordinate-dues-processing-logistics`** — Administrative dues-tracking/reminder logistics (never fee-waiver)
- **`:coordinate-supply-request`** — Non-content consumables
- **`:schedule-staff-shift-proposal`** — Administrative shift PROPOSAL only (never binding)
- **`:flag-safety-concern`** — Facility/member-conduct concerns for HUMAN review (always escalates)

## Three HARD, Permanent, Un-Overridable Governor Checks

1. **Member/event-record unverified** — Target must exist in store AND be independently `:registered?`/`:verified?`, re-derived every time.

2. **Effect not `:propose`** — Rejected outright. All effects must be `:propose`.

3. **Scope exclusion** — Proposals touching collective-bargaining positions, grievance-adjudication, strike-authorization, union-leadership/officer decisions, or disciplinary action are permanently blocked (EN+JA substring scan).

## Test Coverage (20 Cases)

**Store** (5):
- Member lookup
- All members
- Event lookup
- Account lookup
- Ledger append

**Governor** (7):
- Member verified check
- Member unverified check
- Effect not `:propose` check
- Scope exclusion: collective bargaining
- Scope exclusion: grievance
- Scope exclusion: strike
- Safety concern allowed (legitimate use)

**Operation** (5):
- Event scheduling (happy path)
- Unverified member rejection
- Safety concern escalation
- Dues logistics (happy path)
- Supply request (happy path)

**Phase** (3):
- Phase consistency 1
- Phase consistency 2
- Phase consistency 3

## Demo Data

- **Members**: Alice Tanaka (verified), Bob Suzuki (verified), Carol Yamamoto (unverified for testing)
- **Events**: Monthly General Assembly, New Member Training
- **Accounts**: Staff/volunteer assignments

## Limitations

- Deterministic advisor is demo-only; production requires real LLM with prompt injection safeguards.
- MemStore is in-memory; production requires persistent backing store (EDN file, database, ledger).
- Staff-shift proposals are administrative PROPOSAL only; actual shift binding/enforcement requires separate governance gate.

## License

AGPL-3.0
