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

With an empty `REACT_APP_API_BASE_URL`, the browser calls `/api/v1` on the frontend origin. During local development, the development server forwards those requests to `FRONTEND_DEV_PROXY_TARGET`. The backend Maven build bundles the production frontend into the Spring Boot JAR, serving both the UI and `/api/v1` at `http://localhost:8080`; no separate frontend server or reverse proxy is needed for that setup. To call a separate backend origin, set `REACT_APP_API_BASE_URL` (for example, `https://api.example.com`) **before running the build**; React embeds that value in the generated files, so changing it requires a rebuild. The backend must allow the frontend origin. The frontend service is currently commented out in Compose, so it is started separately for local use.

## Checks

```powershell
npm.cmd test -- --watchAll=false
npm.cmd run build
```

Playwright is selected in the architecture but has not yet been configured in this package. Browser journeys must be checked manually until that setup exists.
