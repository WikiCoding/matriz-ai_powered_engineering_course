# Wireframe image prompts

Built-in image-generation tool; source: previous `wireframe.png`. Final asset: `wireframe.png`.

## Initial edit prompt

Use case: ui-mockup. Edit target: existing Matriz grayscale wireframe. Replace it with a coherent, readable large landscape wireframe overview board, preserving Matriz identity, restrained grayscale, thin borders, simple sans-serif UI, white background. Six clearly separated panels in 3 columns by 2 rows, with generous space and readable labels. Title 'Matriz — Revised MVP wireframes'. English only. No code, stack names, technical architecture, invented features, charts, kanban, archive, QA status, registration or password recovery.
Panel 1 '01 Sign in': Matriz, Email field, Password field, Sign in button, message 'Your session has expired. Please sign in again.'
Panel 2 '02 Home': top navigation Home / Projects and user Alex Santos, Sign out. Heading 'Accessible projects'. Three simple count cards 'TODO 2', 'IN_PROGRESS 1', 'DONE 1'. Project row 'Mobile App' with 'Open project'.
Panel 3 '03 Projects': Search projects by name input, New project button. Table Project / Team / Owner; row Mobile App / Atlas / Alex Santos, row Website / Orbit / Morgan Lee. No implication of global visibility. Small create/edit form with Name, Description, Team, Save and Cancel. Owner is read-only.
Panel 4 '04 Project detail': breadcrumb Projects / Mobile App. 'Edit project' and 'Delete project'. Project ID MAT, Team Atlas, Owner Alex Santos, short description. New task button. Filters 'Status: All' and 'Priority: All'. Table ID / Task / Effort / Status / Priority / Assignees. Rows: MAT-101 | Build login | 3 | TODO | HIGH | Ana, Bruno; MAT-102 | API contract | 5 | IN_PROGRESS | MEDIUM | Carla; MAT-103 | Release notes | 1 | DONE | LOW | Diego; MAT-104 | Review forms | 2 | TODO | MEDIUM | Ana. Four rows match home counts.
Panel 5 '05 Task detail / edit': breadcrumb Mobile App / MAT-101. Title 'Build login'. Description field, Status TODO dropdown, Priority HIGH dropdown, Effort 3 dropdown. Project Mobile App; Team Atlas (read-only). Created and Updated date placeholders. 'Assignees' with separate Ana x and Bruno x chips and 'Add assignee' button. 'Save changes' and 'Delete task'. Comments list: 'Ana · 29 Oct — Ready for review.' New comment text area and 'Add comment'. No comment delete or edit actions.
Panel 6 '06 Confirmation & feedback': small dialog 'Delete project?' body 'Delete Mobile App?' buttons Cancel / Delete. Then separate reusable feedback samples 'Loading…', 'No tasks match these filters.', 'Changes saved.', 'Could not save. Your changes are preserved.' and disabled button 'Saving…'. Below show compact mobile navigation example Home / Projects and stacked task card.
Outside UI panels a concise annotation footer: 'Actions shown for authorized users. Sharing, assignee eligibility, comment permissions and deletion policy remain pending. Illustrative data.' This is a design annotation, not product UI.
Keep all content within frames and legible, simplify icons rather than text. High-resolution clean wireframe board, not a screenshot of code.

## Correction prompt

Edit this wireframe board with only these corrections in panel 05 Task detail / edit. Make Build login an editable text input labeled Title. Below the existing comment, put a text area 'Add a comment…' and a distinct 'Add comment' button. Under that put a separate 'Save changes' button for the task form. Keep only the existing top-right Delete task action and remove the redundant bottom Delete task button to make room. Created and Updated dates must be plain read-only text '29 Oct 2026', without calendar/input styling. Keep all other panels, data, labels, layout and grayscale style unchanged. Prioritize legible text and no overlaps.
