# Matriz frontend

React UI for the revised MVP wireframe. The application includes sign-in, a role-specific home, paginated project and task lists, project/task editing, assignment and comments, and administrator user/team creation and lists. The backend remains responsible for authorization and validation.

## Source layout

- `src/App.js` defines pages, routes, and role-based route guards.
- `src/sessions/` owns session storage, profile loading, sign-in, sign-out, and the authenticated page boundary.
- `src/pages/` contains one file per screen: login, home, projects, project detail, task detail, users, and teams.
- `src/components/` contains shared presentation controls and forms.
- `src/api/` contains the HTTP client and feature-specific endpoint functions and paths.
- `src/hooks/` contains reusable loading and mutation state.
- `src/constants.js` contains task choices and empty form values.

## Local development

Start the backend and its supporting services from the repository root using the commands in the root README, then run from this directory:

```powershell
npm.cmd ci
npm.cmd start
```

The development server runs at `http://localhost:3000`. Copy `.env.example` to `.env.local` or set these environment variables in the service environment:

| Variable | Purpose | Default |
| --- | --- | --- |
| `FRONTEND_DEV_PROXY_TARGET` | Target for `/api/v1` requests through the React development server | `http://localhost:8080` |
| `REACT_APP_API_BASE_URL` | Backend origin used by browser requests; omit `/api/v1` | Empty (same origin) |

With an empty `REACT_APP_API_BASE_URL`, the browser calls `/api/v1` on the frontend origin. During local development, the development server forwards those requests to `FRONTEND_DEV_PROXY_TARGET`. A deployed static build needs a reverse proxy for `/api/v1` when using the same-origin default. To call a separate backend origin, set `REACT_APP_API_BASE_URL` (for example, `https://api.example.com`) **before running the build**; React embeds that value in the generated files, so changing it requires a rebuild. The backend must allow the frontend origin. The frontend service is currently commented out in Compose, so it is started separately for local use.

## Checks

```powershell
npm.cmd test -- --watchAll=false
npm.cmd run build
```

Playwright browser journeys are described below.

## Browser journey

Playwright runs against a separately started frontend and an **isolated, disposable** backend/Keycloak/database environment. Use a Project Manager account and an existing team in that environment; the test creates a uniquely named project through the API and deletes it (and its task) afterward. A process interruption or failed cleanup can leave that project behind. Never point this test at production or a shared data set. A fresh local Keycloak volume may require the initial demo password to be changed before login; existing volumes may require the profile reconciliation described in the root README.

From this directory, after `npm.cmd ci` and with the isolated application running:

```powershell
npx.cmd playwright install chromium
$env:E2E_USERNAME = '<project-manager username>'
$env:E2E_PASSWORD = '<project-manager password>'
$env:E2E_TEAM_ID = '<existing team UUID>'
npm.cmd run test:e2e
```

`E2E_BASE_URL` defaults to `http://localhost:3000` and `E2E_API_URL` to `http://localhost:8080`; set both when the isolated services use other addresses. The frontend must target that same backend. The test authenticates through the UI, opens the disposable project, creates a task and checks the listing again after reload. It uses a separate API login for setup and cleanup. Traces are retained on failure in `test-results`; they can contain sensitive test data and should not be committed. Use `npm.cmd run test:e2e -- --list` to check discovery without running the journey.
