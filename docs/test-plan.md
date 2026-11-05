# Task verification plan

Source: requirements, user stories, OpenAPI and the 2026-11-05 decisions in constraints.md. Titles contain 3–150 characters and must not be blank. Project Managers create tasks; both Project Managers and Software Engineers may read all projects and tasks, irrespective of team. Administrators cannot read project/task content. A different team alone is not a denial condition (D-11).

| Scenario and input | Expected result | Level and associated test |
| --- | --- | --- |
| Valid title, including 150 characters | Task retains the title | Unit: `TaskTest.createsTaskWithAllowedEffortAndBoundaryLengthFields` |
| Blank, two-character or 151-character title | Rejected | Unit: `TaskTest.rejectsInvalidTitleDescriptionAndEffort` |
| Manager creates project and task; engineer reads task | 201 on creation, 200 on read with project and effort preserved | Integration: `ApplicationJourneyIntegrationTests.managerCreatesProjectAndTaskAndValidationPreservesTask` |
| Invalid effort on task update | 400 and original effort remains on read-back | Same integration test |
| Engineer or administrator attempts task mutation; administrator reads task | 403 and denied writes leave stored state unchanged | `ApplicationJourneyIntegrationTests.projectAndTaskWritesAreRestrictedToManagers` and `managerCreatesProjectAndTaskAndValidationPreservesTask` |
| Manager signs in, opens a disposable project, creates a uniquely named task | Task appears in the listing and after reload | `matriz-frontend/e2e/task-journey.spec.js` |

The integration tests use disposable PostgreSQL Testcontainers, transaction rollback and simulated JWT decoding; they do not establish live Keycloak behavior. The browser test uses real services and cleans up its own project; interrupted execution may leave data. See the frontend README for isolation and execution instructions. Existing tests were reused where they already covered the exercise rather than duplicated. The integration negative case is invalid effort rather than title, which is already covered at unit level.

## Results, 2026-11-05

- Frontend Jest: 6 suites, 13 tests passed; production build passed.
- Playwright discovery: one journey listed. Running it failed before the test started because the Chromium executable was not installed. Docker was unavailable, so the required isolated application was also unavailable. The browser journey remains unverified.
- Backend `TaskTest`: Maven wrapper failed to start with `Cannot index into a null array`; no test executed. Database-backed tests were not run: Docker daemon was unavailable.
- Live two-account authorization validation and human review are outstanding; see `quality-security-review.md`. These results are not acceptance sign-off.
