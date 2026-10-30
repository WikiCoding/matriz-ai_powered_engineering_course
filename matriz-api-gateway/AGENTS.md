# Gateway agent guide

This guide extends `../AGENTS.md`. Read this component's README and the relevant root architecture, constraints and API contract before changing behavior. The root AAA test rules apply to every test.

## Implementation and review

- Keep the gateway reactive. Avoid blocking calls on request-processing threads and keep business data and domain use cases in the backend.
- Preserve approved routing, authentication, session revocation, trusted-proxy and shared IP-limiting rules. Check the real security/filter chain when changing request handling, not only individual helper methods.
- For affected behavior, cover successful requests, invalid inputs, missing/invalid credentials, denied roles and downstream failures. Security changes need focused checks for token replay after logout, Redis failures and rate-limit bypasses as applicable.
- Simulated Redis/Keycloak responses do not prove live integration, cross-replica behavior or crash durability. Identify which evidence a test provides and what still needs a live check.

## Verification before handoff

Run from `matriz-api-gateway` with Java 21 and the Maven wrapper:

```powershell
.\mvnw.cmd verify
```

This runs compilation, the configured tests and packaging through Maven's verification phase. Do not pass test-skipping flags. Inspect test reports for failures, errors and skipped tests; command success alone does not establish acceptance coverage.

Use a focused check during iteration when relevant:

```powershell
.\mvnw.cmd -Dtest=LoginSecurityTests test
```

The focused check does not replace the full verification gate. Inspect test configuration for service prerequisites; report unavailable services or dependency-resolution failures explicitly. For routing, security or integration changes, exercise the affected HTTP flow in the complete local Compose environment when available, including its rejection paths. Never report mocked checks as live Keycloak/Redis verification.

For documentation-only edits, follow the root verification exception. Report commands, outcomes, skipped checks and remaining risks.
