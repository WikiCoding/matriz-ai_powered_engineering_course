# Backend agent guide

This guide extends `../AGENTS.md`. Read this component's README and relevant requirements, user stories, architecture, constraints, data model and API contract. The root AAA test rules apply to every test.

## Implementation and review

- Preserve the User, Team and Project Management boundaries and inward dependencies. Keep domain rules independent of Spring and persistence; access other modules through explicit interfaces.
- Derive tests from the affected acceptance criteria. Use focused domain/use-case tests for rules and Mockito for dependency isolation; use Spring Boot/PostgreSQL Testcontainers tests for persistence and transactional behavior.
- Check validation, role/resource authorization, successful write/read-back and rejected-operation side effects as applicable. Cover atomic assignment cleanup, cascade deletion and concurrency constraints when those workflows change.
- Use application Flyway migrations in persistence tests. Never modify an applied migration; add a forward migration. For migration changes, verify empty-database initialization, upgrade with retained records and integrity constraints. Keep Keycloak data outside application migrations.

## Verification before handoff

Run from `matriz-backend` with Java 21 and the Maven wrapper. During iteration, verify module boundaries without Docker:

```powershell
.\mvnw.cmd -Dtest=ModuleStructureTests test
```

For the full verification gate, Docker must be running for PostgreSQL Testcontainers:

```powershell
.\mvnw.cmd verify
```

This runs compilation, the configured tests and packaging through Maven's verification phase, including the documented `test` lifecycle phase. Do not pass test-skipping flags. Inspect reports for failures, errors and skipped tests. The module-only check does not establish persistence or full-suite success.

Use disposable test containers; do not redirect tests to a user's persistent Compose database. If Docker or another prerequisite is unavailable, run independent checks that remain possible and report the full gate as blocked, not passed. For cross-component behavior, verify the affected API/UI journey against local Compose when available.

For documentation-only edits, follow the root verification exception. Report commands, outcomes, acceptance coverage and any unverified behavior.
