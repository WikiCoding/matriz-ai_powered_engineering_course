# Constraints and pending decisions

## Known constraints

- Application and documentation are in English.
- The expected user population is approximately 500.
- Docker and AWS are required technical contexts.
- The initial product scope is a task list per project.
- A project has one team; each task has one project and displays that project's team.
- Records must not be permanently deleted; projects can be archived.

## Security and privacy requirements

- Authentication uses email and password.
- Authorization must enforce the Administrator, Project Manager, and Software Engineer permissions defined in the requirements.
- No privacy, data-retention, password-policy, audit-log, or regulatory requirements have been supplied; these must be defined before release.

## Decision register

This is the authoritative list of pending decisions. **Approved** means explicitly accepted; **proposed** means a draft option only; **pending** means unresolved. All items below are pending, including approval of proposed designs. Block only the affected work; do not infer approval from a diagram or API schema. Record the outcome here when a decision is approved and update affected documents.

| ID   | Decision required                                                                                                                                                              | Blocks                                                               |
|------|--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|----------------------------------------------------------------------|
| D-01 | Approve or revise the modular monolith, module boundaries, application stack, relational engine, and draft API/data contracts.                                                 | Application scaffolding and affected contract/schema implementation. |
| D-02 | Confirm identity provider (Keycloak proposed), credential provisioning, role authority/synchronization, and email uniqueness. OIDC and provider-managed passwords can coexist. | Login, user creation, role enforcement, and identity schema.         |
| D-03 | Decide user-to-team cardinality and who manages the team lifecycle.                                                                                                            | Membership schema/updates and team administration.                   |
| D-04 | Decide project/task identifier generation and format; identifiers must remain unique.                                                                                          | Identifier creation and validation.                                  |
| D-05 | Decide whether assignees must belong to the project's team, including after project/team changes.                                                                              | Assignment and reassignment validation.                              |
| D-06 | Define archived project visibility/editability and task behavior; decide whether tasks can be archived and by whom.                                                            | Archival workflows and related reads/updates.                        |
| D-07 | Define timestamp format and timezone.                                                                                                                                          | Timestamp storage and API/display contracts.                         |
| D-08 | Confirm or revise the proposed 99.95% SLA, measurement window/scope, and performance targets.                                                                                  | Availability design and performance acceptance checks.               |
| D-09 | Select AWS services, identity hosting, network topology, backups/recovery, and monitoring.                                                                                     | Deployment design and production readiness.                          |
| D-10 | Define password, recovery, session, lockout, privacy, retention, audit, and regulatory requirements.                                                                           | Relevant security behavior and release approval.                     |

## Scope limitations

No backlog, Kanban board, report, analytics feature, or external integration is included in the MVP. Architecture, data model, and API documents are proposals supporting that scope; implementation depends on the relevant decisions above.
