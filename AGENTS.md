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
