import { Link, NavLink } from "react-router-dom";
import Message from "./Message";

function Shell({ profile, signOut, error, children }) {
  const admin = profile.role === "ADMINISTRATOR";
  const links = admin
    ? [
        ["/", "Home"],
        ["/users", "Users"],
        ["/teams", "Teams"],
      ]
    : [
        ["/", "Home"],
        ["/projects", "Projects"],
      ];
  return (
    <>
      <header className="topbar">
        <Link className="brand" to="/">
          Matriz
        </Link>
        <nav aria-label="Main navigation">
          {links.map(([path, label]) => (
            <NavLink key={path} end to={path}>
              {label}
            </NavLink>
          ))}
        </nav>
        <div className="account">
          <span>{profile.displayName || profile.email}</span>
          <button className="quiet" onClick={signOut}>
            Sign out
          </button>
        </div>
      </header>
      <main className="container">
        <Message error={error} />
        {children}
      </main>
      <nav className="mobile-nav" aria-label="Mobile navigation">
        {links.map(([path, label]) => (
          <NavLink key={path} end to={path}>
            {label}
          </NavLink>
        ))}
      </nav>
    </>
  );
}
export default Shell;
