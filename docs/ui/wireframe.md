# Revised MVP wireframe

[Open the wireframe board](wireframe.png). Updated 2026-10-29; a visual design artifact, not implemented UI.

## Screens and reusable patterns

1. Sign in: email/password authentication and expired-session feedback. Authentication is Keycloak-owned; the panel represents the login experience, not an independent application password store.
2. Home: authenticated profile/sign-out, accessible projects and task counts by status.
3. Projects: name search, paginated alphabetical-by-name list and create/edit form with team and read-only owner.
4. Project detail: project metadata, edit/delete actions, task creation, status/priority filters, creation-date sorting, a paginated task table and zero or one Software Engineer assignee.
5. Task detail/edit: title, description, status, priority, effort, derived team, read-only timestamps, a single optional assignee selector/removal control and comments with author/date. Posting a comment has its own action, separate from saving task edits.
6. Shared patterns: delete confirmation, loading/empty/success/error feedback, disabled pending action, compact mobile navigation and task card.

Reuse navigation, fields, buttons, dialogs, badges, assignment chips, comments and feedback components. Task creation reuses task fields; existing comments and timestamps belong to saved tasks. Do not display UUIDs or separate business identifier fields. Status choices are TODO / IN_PROGRESS / DONE; priorities LOW / MEDIUM / HIGH. Counts in the sample match the four illustrated tasks.

## Interpretation and pending decisions

- All names, records and dates are fictitious examples. Use the server zone and dd/MM/yyyy HH:mm:ss; use a 24-hour clock.
- Screens show potential actions for users authorized to perform them, not permissions granted to every role. Only Software Engineers create comments; Project Managers manage the optional Software Engineer assignee. All Project Managers and Software Engineers can access every project; assignees must be in the project's team. Project Managers and Software Engineers read comments; Administrators only see administration workflows.
- The deletion dialog must explain permanent removal of the project/tasks and dependent comments/assignments.
- Owner and comment author are backend-derived. No ownership-transfer control is added.
- The board focuses on changed project/task journeys. Existing administrator user/team membership requirements remain in force; the board is not a complete administration design. The administration user list is paginated and sorts by email; the team list is paginated and sorts alphabetically by name.
- List controls include pagination on tasks, projects, teams and users. Tasks sort by creation date and filter by status (with the existing priority filter); projects and teams sort alphabetically by name; users sort by email. The current PNG does not yet depict these controls, so these textual notes take precedence.
- No new authentication policies, comment editing/deletion, Kanban, analytics or registration flows are introduced.
- The API baseline is approved and updated for current decisions. This wireframe does not approve unresolved public contracts or business rules.

## Asset provenance

Edited with the built-in image-generation tool from the previous wireframe, then visually reviewed. Exact generation and correction prompts are in [wireframe-prompts.md](wireframe-prompts.md).

## Latest decision precedence

The PNG has not been regenerated and may show multiple assignees, older dates, or list controls that predate pagination/sorting. These notes and requirements.md take precedence. Physical deletion is confirmed; dependent tasks, comments and assignments are deleted, preserving users and teams. shadcn/ui is an approved UI option.
