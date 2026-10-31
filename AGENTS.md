# Matriz agent guide

## Before working

Read `README.md` and the relevant files in `docs/` before starting. Treat documented requirements, constraints, scope limits, and pending decisions as the source of truth.

`docs/constraints.md` is the authoritative pending-decision register. Proposed designs are not approval to implement them. If documents conflict, flag the conflict and pause only the affected work until it is resolved.

## Run and test commands

From the repository root, start the complete local application with:

```text
docker compose --profile prod up
```

Use `docker compose --profile prod up --build` when application images must be rebuilt. Stop the local environment while preserving volumes with `docker compose --profile prod down`.

From `matriz-backend`, run the module-boundary test without Docker:

```text
mvnw.cmd -Dtest=ModuleStructureTests test
```

Run the complete backend test suite with Docker running, because integration tests start a PostgreSQL Testcontainers container:

```text
mvnw.cmd test
```

## Working rules

- Do not add features, business rules, or technical decisions that are not approved in the documentation.
- Routine, reversible implementation choices within approved requirements and architecture may be made autonomously. New dependencies, platform choices, public contracts, and changes to security or data ownership require approval.
- For significant changes, present a short plan before editing.
- Change only files that are necessary for the requested task; preserve unrelated work.
- Ask for approval before destructive, sensitive, external, or irreversible actions.
- Never expose credentials, tokens, personal data, or other confidential information.
- If a required decision is pending, state the dependency and ask for direction rather than guessing.
- Run checks appropriate to the change before handoff; cover affected acceptance criteria when implementing behavior. Use documented setup and check commands when available; do not invent successful checks.

## Handoff

Report assumptions, files changed, check results, and any checks that could not run with the reason. Keep documentation current when an approved change affects requirements, constraints, architecture, scope, or user stories.

## Feature development workflow

1. Read this guide and the affected component's `AGENTS.md` and README. Identify the requested user stories, acceptance criteria, approved contracts and relevant decisions before editing.
2. For significant work, present a short plan covering affected components, implementation steps and verification. Map changed behavior to acceptance checks, including relevant validation, permissions, failure cases and persistence. Resolve only genuinely pending decisions; implementation details explicitly delegated in `docs/constraints.md` do not require renewed approval.
3. Implement the smallest complete change within the authorized scope. Keep module ownership and dependency boundaries intact. Update affected contracts and documentation alongside implementation; revise the pagination API baseline before implementing collection listings.
4. Add or update meaningful automated tests for changed behavior. For bug fixes, reproduce the defect with a regression test where practical. Test observable outcomes, not merely the implementation's internal structure.
5. Review the diff as a separate verification pass: compare it with acceptance criteria, check failure paths and authorization, inspect test quality and remove accidental unrelated changes. A self-review is not an independent review.
6. Run full verification only when the user clearly prompts for it, to conserve token usage. Fix failures introduced by the change and rerun affected checks. For changes crossing components, also verify the affected local application journey when the environment is available when full verification is requested.
7. Hand off with evidence: list the commands run and their outcomes, covered acceptance criteria, skipped tests, failures and blockers. Do not claim completion of verification when required checks are failing or unavailable. Distinguish pre-existing failures from regressions using evidence.

## Test style: Arrange, Act, Assert (AAA)

- All tests shall follow **Arrange, Act, Assert**, in that order. Apply this to unit, integration, module, component and browser tests. All new or modified tests must comply; bring existing tests into compliance when touching them without unrelated bulk rewrites.
- **Arrange:** create fixtures, inputs, dependency stubs and required initial state.
- **Act:** execute the behavior under test, with one focused action or coherent workflow per test.
- **Assert:** verify observable results, state changes and relevant side effects, including absence of changes after rejected operations.
- Mark the sections with `// Arrange`, `// Act` and `// Assert` (or equivalent comments for the language). Shared setup may hold common arrangement; make the test-specific setup clear. For framework-managed actions such as context startup, explicitly identify the lifecycle action rather than inventing an empty action.
- Exception assertions and reactive assertion APIs may execute the action inside an assertion callback or verification chain. Keep setup separate and make the Act/Assert boundary clear; do not break correct framework usage solely to separate statements.
- Keep tests deterministic and isolated. Use controlled time, unique disposable data and bounded asynchronous waits instead of arbitrary sleeps. Never rely on execution order or real credentials.
- Do not weaken assertions, disable tests or skip execution just to obtain a passing result. A successful build, mocked integration or manual smoke check does not substitute for the other required verification.

## Verification scope

Component guides extend this root guide and specify commands from their own directories. On Windows PowerShell, use `.\mvnw.cmd` and `npm.cmd`; use `./mvnw` and `npm` on other platforms.

For documentation-only changes, verify paths, commands against current build configuration, consistency and diff whitespace; application builds and test suites are not required unless executable behavior or configuration also changes. Report that distinction explicitly. Do not add dependencies or test infrastructure silently to satisfy a missing check; follow existing approvals and report any remaining setup dependency.
