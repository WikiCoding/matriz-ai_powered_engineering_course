# Frontend agent guide

This guide extends `../AGENTS.md`. Read this component's README, relevant requirements and user stories, the API contract and `../docs/ui/wireframe.md`. Current textual decisions take precedence over the older wireframe image. The root AAA test rules apply to every test.

## Implementation and review

- Follow the approved React structure: reusable components, centralized API access, local state and optional Context. Keep role checks in the UI consistent with backend policy; hidden controls do not provide authorization.
- Test user-visible behavior with Jest and the existing Testing Library dependencies. Arrange rendered components and API responses, Act through user interactions, and Assert the visible result and relevant requests.
- For affected flows, verify loading, empty, success and error states, retained form data on failure, prevention of duplicate submissions and updates after API confirmation. Check navigation, keyboard access and relevant screen widths.
- Keep assertions focused on behavior instead of component internals. Await asynchronous updates; do not use arbitrary sleeps or rely solely on snapshots.

## Verification before handoff

Run from `matriz-frontend`. Use the committed lockfile with `npm.cmd ci` when dependency installation is needed; do not alter dependencies or regenerate the lockfile merely to run checks.

Run all Jest tests once, without interactive watch mode, then build the application:

```powershell
npm.cmd test -- --watchAll=false
npm.cmd run build
```

Both checks must pass for code changes. Inspect test output for skipped tests and build output for warnings; report unresolved findings. A successful build alone does not prove behavior or test success. There is currently no separate lint or type-check script in `package.json`; do not claim those checks ran.

Playwright is selected in the architecture but is not currently installed or configured here. For browser-journey changes, use the repository's Playwright command once that setup exists. Until then, explicitly report the automated browser coverage gap and perform the affected manual journey against local Compose when available; manual checks do not replace automated acceptance coverage. Do not claim a browser suite passed or invent an executable script.

For documentation-only edits, follow the root verification exception. Report commands, outcomes, browser checks performed and remaining verification gaps.
