import { BrowserRouter, Navigate, Route, Routes } from "react-router-dom";
import Shell from "./components/Shell";
import Home from "./pages/Home";
import Projects from "./pages/Projects";
import ProjectDetail from "./pages/ProjectDetail";
import TaskDetail from "./pages/TaskDetail";
import Users from "./pages/Users";
import Teams from "./pages/Teams";
import {
  SessionGate,
  SessionProvider,
  useSession,
} from "./sessions/SessionProvider";
import "./App.css";

function AppRoutes() {
  const { token, profile, error, signOut } = useSession();
  const admin = profile.role === "ADMINISTRATOR";

  return (
    <Shell profile={profile} signOut={signOut} error={error}>
      <Routes>
        <Route path="/" element={<Home token={token} profile={profile} />} />
        <Route
          path="/projects"
          element={
            admin ? (
              <Navigate to="/" />
            ) : (
              <Projects token={token} role={profile.role} />
            )
          }
        />
        <Route
          path="/projects/:projectId"
          element={
            admin ? (
              <Navigate to="/" />
            ) : (
              <ProjectDetail token={token} role={profile.role} />
            )
          }
        />
        <Route
          path="/tasks/:taskId"
          element={
            admin ? (
              <Navigate to="/" />
            ) : (
              <TaskDetail token={token} role={profile.role} />
            )
          }
        />
        <Route
          path="/users"
          element={admin ? <Users token={token} /> : <Navigate to="/" />}
        />
        <Route
          path="/teams"
          element={admin ? <Teams token={token} /> : <Navigate to="/" />}
        />
        <Route path="*" element={<Navigate to="/" />} />
      </Routes>
    </Shell>
  );
}

export default function App() {
  return (
    <BrowserRouter
      future={{ v7_startTransition: true, v7_relativeSplatPath: true }}
    >
      <SessionProvider>
        <SessionGate>
          <AppRoutes />
        </SessionGate>
      </SessionProvider>
    </BrowserRouter>
  );
}
