# Security Policy

## Scope Boundary Enforcement

The three HARD governor checks are the primary security boundary:

1. **Member verification** — Ensures only verified union members can initiate operations.
2. **Propose-only enforcement** — Prevents binding decisions or approvals.
3. **Scope exclusion** — Blocks proposals touching collective bargaining, grievance adjudication, strike authorization, union leadership decisions, or disciplinary action.

All three checks are **structural** (enforced by code) with **no human override path**.

## Proposal Field Preservation

Original proposal fields are preserved in all operations. The scope-exclusion check scans the entire proposal (including user-provided reason fields) to detect scope violations embedded in data.

This fix (ADR-2607154302) addresses the isic-920 bug where a scope-exclusion check was computed but never used, allowing violations to pass silently.

## Prompt Injection Safeguards

Current advisor is deterministic demo; production requires:
- Real LLM integration with prompt injection detection.
- Confirmation that advisor reasoning does not influence governor decision (advisor is advisory only).
- Validation that proposal field content cannot escape scope-exclusion patterns.

## Reporting Security Issues

Use GitHub Security Advisories to report vulnerabilities confidentially.

## Demo Limitations

- MemStore is in-memory; production requires persistent, audit-logged backing store.
- Advisor is deterministic demo; production requires real LLM with safeguards.
- No real authentication/authorization; production requires CACAO self-mint or equivalent.
