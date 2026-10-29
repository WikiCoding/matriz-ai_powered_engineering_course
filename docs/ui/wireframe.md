# Revised MVP wireframe

[Open the wireframe board](wireframe.png). Updated 2026-10-29; a visual design artifact, not implemented UI.

## Screens and reusable patterns

1. Sign in: email/password authentication and expired-session feedback. Authentication is Keycloak-owned; the panel represents the login experience, not an independent application password store.
2. Home: authenticated profile/sign-out, accessible projects and task counts by status.
3. Projects: name search, list and create/edit form with team and read-only owner.
4. Project detail: project metadata, edit/delete actions, task creation, status/priority filters and multiple assignees in the task table.
5. Task detail/edit: title, description, status, priority, effort, derived team, read-only timestamps, separate assignee chips/removal controls and comments with author/date. Posting a comment has its own action, separate from saving task edits.
6. Shared patterns: delete confirmation, loading/empty/success/error feedback, disabled pending action, compact mobile navigation and task card.

Reuse navigation, fields, buttons, dialogs, badges, assignment chips, comments and feedback components. Task creation reuses task fields; existing comments and timestamps belong to saved tasks. Identifier input/generation remains D-04. Status choices are TODO / IN_PROGRESS / DONE; priorities LOW / MEDIUM / HIGH. Counts in the sample match the four illustrated tasks.

## Interpretation and pending decisions

- All names, records and dates are fictitious examples. Date display does not settle D-07.
- Screens show potential actions for users authorized to perform them, not permissions granted to every role. Sharing, comments and assignment-management permissions remain D-11; eligibility remains D-05. The second project owner is illustrative of a possible authorized list, not approval of a sharing rule.
- The deletion dialog deliberately makes no claims about permanent deletion, cascading or recovery; final explanatory copy depends on D-06.
- Owner and comment author are backend-derived. No ownership-transfer control is added.
- The board focuses on changed project/task journeys. Existing administrator user/team membership requirements remain in force; the board is not a complete administration design.
- No pagination, new authentication policies, comment editing/deletion, Kanban, analytics or registration flows are introduced.
- The API contract remains outdated; reconcile it before implementation. This wireframe does not approve unresolved public contracts or business rules.

## Asset provenance

Edited with the built-in image-generation tool from the previous wireframe, then visually reviewed. Exact generation and correction prompts are in [wireframe-prompts.md](wireframe-prompts.md).
