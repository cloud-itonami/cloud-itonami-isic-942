# Contributing

## Reporting Issues

- **Scope violations** (proposals that should be blocked by the three HARD governor checks)
- **Test failures** (any test case that fails unexpectedly)
- **Documentation gaps**

## Pull Requests

1. Branch from `main`.
2. Add test cases for new behavior.
3. Ensure all 20 tests pass via `nbb test_runner.cljs`.
4. Keep scope strictly within administrative coordination.

## Scope Boundaries (Non-Negotiable)

This actor **does not** handle:
- Collective bargaining positions or negotiations
- Grievance adjudication or dispute resolution
- Strike authorization or labor actions
- Union leadership or officer elections
- Disciplinary action or member expulsion

Violations are caught by the three HARD governor checks (no human override path).
