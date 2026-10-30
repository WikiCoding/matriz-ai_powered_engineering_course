# Backend scaffold

The backend has three closed Spring Modulith modules under `com.wikicoding.matrizbackend`: `user`, `team`, and `projectmanagement`. Public types in each module root form its API. Nested packages are internal; use explicit module interfaces for cross-module access.

Each module contains documented packages for `domain`, `application`, `adapter.in.web`, `adapter.out.persistence`, and `configuration`. Project Management has separate domain packages for projects, tasks, assignments, and comments. These are package scaffolds, not implemented use cases or entities. Domain code must remain independent of Spring and persistence frameworks.

## PostgreSQL configuration

`application.properties` uses `DB_URL` for a complete PostgreSQL JDBC URL, defaulting to `jdbc:postgresql://localhost:5432/matriz`, and `APP_DB_USER` (default `matriz`). Set `APP_DB_PASSWORD` to the password configured for the Compose `app-db` service. No password is embedded in the backend configuration.

Standard Spring overrides `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`, and `SPRING_DATASOURCE_PASSWORD` also work. For a backend running inside Compose, set `DB_URL=jdbc:postgresql://app-db:5432/matriz`. A Compose `.env` file does not automatically populate an IDE or terminal process environment; configure those variables separately.

Flyway runs automatically at backend startup using the configured application datasource. V1 creates teams, application profiles/provisioning state, projects, tasks, assignments and comments. The backend does not use Spring Modulith's persistent JDBC event-publication registry or dedicated audit storage. SQL script initialization is disabled. Flyway validates checksums, disables clean, and does not automatically baseline an existing unmanaged database. Add a new versioned migration for future changes; never edit an applied script. No seed data or Keycloak tables are included.

## Verification

From `matriz-backend`, run `mvnw.cmd -Dtest=ModuleStructureTests test` on Windows (or `./mvnw -Dtest=ModuleStructureTests test` elsewhere). The test checks the three detected modules and verifies Modulith boundaries without starting Spring or requiring PostgreSQL. Run `mvnw.cmd test` (or `mvn test` with installed Maven) with Docker running for the full suite. The Spring Boot integration tests use a disposable PostgreSQL 16.4 container, apply the same migration on startup, and cover repeat migration, checksum mismatch detection, constraints and cascades. They do not migrate the local Compose database.

The existing POM pins Spring Modulith 2.1.1, while `docs/architecture.md` records 2.1.2. Dependency alignment is outstanding; this scaffolding change preserves the existing dependency versions.
