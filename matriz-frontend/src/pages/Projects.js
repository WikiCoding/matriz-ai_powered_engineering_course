import { useState } from "react";
import { Link } from "react-router-dom";
import { projectsPath, createProject } from "../api/projects";
import { teamOptionsPath } from "../api/teams";
import { useData } from "../hooks/useData";
import { useMutation } from "../hooks/useMutation";
import Message from "../components/Message";
import ProjectForm from "../components/ProjectForm";
import DataTable from "../components/DataTable";
import Pager from "../components/Pager";
import { newProject } from "../constants";

function Projects({ token, role }) {
  const manager = role === "PROJECT_MANAGER";
  const [page, setPage] = useState(0),
    [version, setVersion] = useState(0),
    [show, setShow] = useState(false),
    [form, setForm] = useState(newProject);
  const projects = useData(projectsPath(page), token, version),
    teams = useData(teamOptionsPath, token);
  const mutation = useMutation();
  async function submit(e) {
    e.preventDefault();
    if (
      await mutation.run(() => createProject(token, form), "Project created.")
    ) {
      setShow(false);
      setForm(newProject);
      setVersion((v) => v + 1);
    }
  }
  return (
    <>
      <div className="heading">
        <div>
          <p className="eyebrow">WORKSPACE</p>
          <h1>Projects</h1>
          <p className="muted">Browse projects in alphabetical order.</p>
        </div>
        {manager && (
          <button className="primary" onClick={() => setShow(true)}>
            + New project
          </button>
        )}
      </div>
      <Message
        error={projects.error || teams.error || mutation.error}
        success={mutation.success}
      />
      {show && (
        <ProjectForm
          title="New project"
          value={form}
          change={setForm}
          teams={teams.data?.content}
          submit={submit}
          cancel={() => setShow(false)}
          pending={mutation.pending}
        />
      )}
      <DataTable
        data={projects}
        columns={["Project", "Description"]}
        rows={(p) => [
          <Link to={`/projects/${p.id}`}>{p.name}</Link>,
          p.description,
        ]}
      />
      <Pager data={projects.data} page={page} change={setPage} />
    </>
  );
}
export default Projects;
