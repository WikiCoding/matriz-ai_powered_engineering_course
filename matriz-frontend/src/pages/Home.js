import { Link } from "react-router-dom";
import { dashboardPath } from "../api/dashboard";
import { useData } from "../hooks/useData";
import Message from "../components/Message";
import { statuses } from "../constants";

function Home({ token, profile }) {
  const admin = profile.role === "ADMINISTRATOR";
  const dashboard = useData(admin ? null : dashboardPath, token);
  return (
    <>
      <div className="heading">
        <div>
          <p className="eyebrow">OVERVIEW</p>
          <h1>Welcome, {profile.displayName || profile.email}</h1>
          <p className="muted">Your workspace at a glance.</p>
        </div>
      </div>
      {admin ? (
        <div className="cards">
          <Link className="card" to="/users">
            <h2>Users</h2>
            <p>Create and view accounts</p>
            <b>Open users →</b>
          </Link>
          <Link className="card" to="/teams">
            <h2>Teams</h2>
            <p>Create and view teams</p>
            <b>Open teams →</b>
          </Link>
        </div>
      ) : (
        <>
          <Message error={dashboard.error} />
          {dashboard.loading ? (
            <p role="status">Loading…</p>
          ) : (
            dashboard.data && (
              <>
                <div className="stats">
                  {statuses.map((s) => (
                    <div className="stat" key={s}>
                      <span>{s.replace("_", " ")}</span>
                      <strong>{dashboard.data.taskCounts?.[s] || 0}</strong>
                    </div>
                  ))}
                </div>
                <div className="section-title">
                  <h2>Accessible projects</h2>
                  <Link to="/projects">View all projects →</Link>
                </div>
                <div className="cards">
                  {dashboard.data.projects?.map((project) => (
                    <Link
                      className="card"
                      key={project.id}
                      to={`/projects/${project.id}`}
                    >
                      <h3>{project.name}</h3>
                      <p>{project.description}</p>
                      <b>Open project →</b>
                    </Link>
                  ))}
                </div>
                {!dashboard.data.projects?.length && (
                  <p className="empty">No projects yet.</p>
                )}
              </>
            )
          )}
        </>
      )}
    </>
  );
}
export default Home;
