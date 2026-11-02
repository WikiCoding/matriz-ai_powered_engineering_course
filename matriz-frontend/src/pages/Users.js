import { useState } from "react";
import { usersPath, createUser } from "../api/users";
import { teamOptionsPath } from "../api/teams";
import { useData } from "../hooks/useData";
import { useMutation } from "../hooks/useMutation";
import Message from "../components/Message";
import DataTable from "../components/DataTable";
import Pager from "../components/Pager";
import Section from "../components/Section";
import Field from "../components/Field";
import { choices } from "../constants";

function Users({ token }) {
  const [page, setPage] = useState(0),
    [version, setVersion] = useState(0),
    [form, setForm] = useState({
      username: "",
      email: "",
      firstName: "",
      lastName: "",
      password: "",
      role: "SOFTWARE_ENGINEER",
      teamId: "",
    });
  const list = useData(usersPath(page), token, version),
    teams = useData(teamOptionsPath, token),
    mutation = useMutation();
  const update = (key, value) => setForm({ ...form, [key]: value });
  async function submit(e) {
    e.preventDefault();
    if (await mutation.run(() => createUser(token, form), "User created.")) {
      setForm({
        username: "",
        email: "",
        firstName: "",
        lastName: "",
        password: "",
        role: "SOFTWARE_ENGINEER",
        teamId: "",
      });
      setVersion((v) => v + 1);
    }
  }
  return (
    <>
      <div className="heading">
        <div>
          <p className="eyebrow">ADMINISTRATION</p>
          <h1>Users</h1>
          <p className="muted">Accounts are sorted by email.</p>
        </div>
      </div>
      <Message
        error={list.error || teams.error || mutation.error}
        success={mutation.success}
      />
      <Section title="Create user">
        <form onSubmit={submit}>
          <div className="form-grid">
            <Field
              label="First name"
              required
              value={form.firstName}
              onChange={(v) => update("firstName", v)}
            />
            <Field
              label="Last name"
              required
              value={form.lastName}
              onChange={(v) => update("lastName", v)}
            />
            <Field
              label="Username"
              required
              value={form.username}
              onChange={(v) => update("username", v)}
            />
            <Field
              label="Email"
              type="email"
              required
              value={form.email}
              onChange={(v) => update("email", v)}
            />
            <Field
              label="Password"
              type="password"
              minLength="6"
              required
              value={form.password}
              onChange={(v) => update("password", v)}
            />
            <Field
              label="Role"
              value={form.role}
              onChange={(v) => update("role", v)}
              options={choices([
                "ADMINISTRATOR",
                "PROJECT_MANAGER",
                "SOFTWARE_ENGINEER",
              ])}
            />
            <Field
              label="Team"
              required
              value={form.teamId}
              onChange={(v) => update("teamId", v)}
              options={[
                ["", "Select a team"],
                ...(teams.data?.content || []).map((t) => [t.id, t.name]),
              ]}
            />
          </div>
          <div className="actions">
            <button className="primary" disabled={mutation.pending}>
              Create user
            </button>
          </div>
        </form>
      </Section>
      <DataTable
        data={list}
        columns={["Email", "Team"]}
        rows={(u) => [
          u.email,
          teams.data?.content.find((t) => t.id === u.teamId)?.name || "—",
        ]}
      />
      <Pager data={list.data} page={page} change={setPage} />
    </>
  );
}
export default Users;
