# Governance

## Three HARD, Permanent, Un-Overridable Checks

1. **Member/event-record unverified** — Target must exist in store AND be independently `:registered?`/`:verified?`, re-derived every time. No exceptions.

2. **Effect not `:propose`** — All operations must propose (not approve, deny, or execute binding decisions). No exceptions.

3. **Scope exclusion** — Proposals touching collective-bargaining, grievance-adjudication, strike-authorization, union-leadership/officer decisions, or disciplinary action are permanently rejected. No human override path.

These checks cannot be disabled, modified, or overridden by configuration or human decision. They are enforced structurally by the governor module.

## Operations (Closed Allowlist)

Only these operations are allowed:
- `:schedule-member-meeting`
- `:coordinate-dues-processing-logistics`
- `:coordinate-supply-request`
- `:schedule-staff-shift-proposal`
- `:flag-safety-concern`

All other operations are rejected outright.

## Escalation Paths

- **Safety concerns** (`:flag-safety-concern`) always escalate to human review, even if governance passes.
- **Governance hold** (any HARD check failure) escalates for human determination.

## Decision Flow

1. **Intake** — Validate basic proposal structure.
2. **Advise** — Enrich with advisor reasoning (deterministic demo).
3. **Govern** — Apply three HARD checks.
4. **Decide** — Escalate if safety concern, approve if governance passes, hold if governance fails.
5. **Commit** — Execute approved operation (demo: log to ledger).
