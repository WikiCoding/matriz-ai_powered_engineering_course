import { useState } from "react";
import { teamsPath, createTeam } from "../api/teams";
import { useData } from "../hooks/useData";
import { useMutation } from "../hooks/useMutation";
import Message from "../components/Message";
import DataTable from "../components/DataTable";
import Pager from "../components/Pager";
import Section from "../components/Section";
import Field from "../components/Field";

function Teams({ token }) {
  const [page, setPage] = useState(0),
    [version, setVersion] = useState(0),
    [name, setName] = useState("");
  const list = useData(teamsPath(page), token, version),
    mutation = useMutation();
  async function submit(e) {
    e.preventDefault();
    if (await mutation.run(() => createTeam(token, name), "Team created.")) {
      setName("");
      setVersion((v) => v + 1);
    }
  }
  return (
    <>
      <div className="heading">
        <div>
          <p className="eyebrow">ADMINISTRATION</p>
          <h1>Teams</h1>
          <p className="muted">Teams are sorted by name.</p>
        </div>
      </div>
      <Message
        error={list.error || mutation.error}
        success={mutation.success}
      />
      <Section title="Create team">
        <form className="inline" onSubmit={submit}>
          <Field label="Team name" required value={name} onChange={setName} />
          <button className="primary" disabled={mutation.pending}>
            Create team
          </button>
        </form>
      </Section>
      <DataTable data={list} columns={["Team"]} rows={(t) => [t.name]} />
      <Pager data={list.data} page={page} change={setPage} />
    </>
  );
}
export default Teams;
