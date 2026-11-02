import { useState } from "react";
import { Link, useNavigate, useParams } from "react-router-dom";
import {
  projectPath,
  projectTasksPath,
  updateProject,
  deleteProject,
} from "../api/projects";
import { createTask } from "../api/tasks";
import { teamOptionsPath } from "../api/teams";
import { userDisplayPath } from "../api/users";
import { useData } from "../hooks/useData";
import { useMutation } from "../hooks/useMutation";
import Message from "../components/Message";
import ProjectForm from "../components/ProjectForm";
import TaskFields from "../components/TaskFields";
import DataTable from "../components/DataTable";
import Pager from "../components/Pager";
import Badge from "../components/Badge";
import Confirm from "../components/Confirm";
import Section from "../components/Section";
import Field from "../components/Field";
import {
  newProject,
  newTask,
  statuses,
  priorities,
  choices,
} from "../constants";

function ProjectDetail({ token, role }) {
  const { projectId } = useParams(),
    navigate = useNavigate(),
    manager = role === "PROJECT_MANAGER";
  const [page, setPage] = useState(0),
    [status, setStatus] = useState(""),
    [priority, setPriority] = useState(""),
    [version, setVersion] = useState(0),
    [edit, setEdit] = useState(false),
    [create, setCreate] = useState(false),
    [confirm, setConfirm] = useState(false),
    [projectForm, setProjectForm] = useState(newProject),
    [taskForm, setTaskForm] = useState(newTask);
  const project = useData(projectPath(projectId), token, version),
    tasks = useData(
      projectTasksPath(projectId, page, status, priority),
      token,
      version,
    ),
    teams = useData(teamOptionsPath, token),
    owner = useData(
      project.data ? userDisplayPath(project.data.ownerId) : null,
      token,
    );
  const mutation = useMutation();
  async function saveProject(e) {
    e.preventDefault();
    if (
      await mutation.run(() => updateProject(token, projectId, projectForm))
    ) {
      setEdit(false);
      setVersion((v) => v + 1);
    }
  }
  async function saveTask(e) {
    e.preventDefault();
    if (
      await mutation.run(
        () =>
          createTask(token, {
            ...taskForm,
            projectId,
            effort: Number(taskForm.effort),
          }),
        "Task created.",
      )
    ) {
      setCreate(false);
      setTaskForm(newTask);
      setVersion((v) => v + 1);
    }
  }
  async function remove() {
    if (await mutation.run(() => deleteProject(token, projectId)))
      navigate("/projects");
  }
  return (
    <>
      <Link className="back" to="/projects">
        ← Projects
      </Link>
      <Message
        error={
          project.error ||
          tasks.error ||
          teams.error ||
          owner.error ||
          mutation.error
        }
        success={mutation.success}
      />
      {project.loading ? (
        <p role="status">Loading…</p>
      ) : (
        project.data && (
          <>
            <div className="heading">
              <div>
                <p className="eyebrow">PROJECT</p>
                <h1>{project.data.name}</h1>
                <p className="muted">{project.data.description}</p>
              </div>
              {manager && (
                <div className="actions">
                  <button
                    onClick={() => {
                      setProjectForm({
                        name: project.data.name,
                        description: project.data.description,
                        teamId: project.data.teamId,
                      });
                      setEdit(true);
                    }}
                  >
                    Edit project
                  </button>
                  <button
                    className="danger-outline"
                    onClick={() => setConfirm(true)}
                  >
                    Delete project
                  </button>
                </div>
              )}
            </div>
            <div className="meta">
              <div>
                <small>Team</small>
                <strong>
                  {teams.data?.content.find((t) => t.id === project.data.teamId)
                    ?.name || "—"}
                </strong>
              </div>
              <div>
                <small>Owner</small>
                <strong>{owner.data?.displayName || "—"}</strong>
              </div>
            </div>
            {edit && (
              <ProjectForm
                title="Edit project"
                value={projectForm}
                change={setProjectForm}
                teams={teams.data?.content}
                submit={saveProject}
                cancel={() => setEdit(false)}
                pending={mutation.pending}
              />
            )}
          </>
        )
      )}
      <div className="section-title">
        <h2>Tasks</h2>
        {manager && (
          <button className="primary" onClick={() => setCreate(true)}>
            + New task
          </button>
        )}
      </div>
      <div className="filters">
        <Field
          label="Status"
          value={status}
          onChange={(v) => {
            setStatus(v);
            setPage(0);
          }}
          options={[["", "All statuses"], ...choices(statuses)]}
        />
        <Field
          label="Priority"
          value={priority}
          onChange={(v) => {
            setPriority(v);
            setPage(0);
          }}
          options={[["", "All priorities"], ...choices(priorities)]}
        />
      </div>
      <DataTable
        data={tasks}
        columns={["Task", "Effort", "Status", "Priority"]}
        rows={(t) => [
          <Link to={`/tasks/${t.id}`}>{t.title}</Link>,
          t.effort,
          <Badge value={t.status} />,
          <Badge value={t.priority} />,
        ]}
        empty="No tasks match these filters."
      />
      <Pager data={tasks.data} page={page} change={setPage} />
      {create && (
        <Section title="New task">
          <form onSubmit={saveTask}>
            <TaskFields value={taskForm} change={setTaskForm} />
            <div className="actions">
              <button type="button" onClick={() => setCreate(false)}>
                Cancel
              </button>
              <button className="primary" disabled={mutation.pending}>
                Create task
              </button>
            </div>
          </form>
        </Section>
      )}
      {confirm && (
        <Confirm
          title="Delete project?"
          close={() => setConfirm(false)}
          submit={remove}
          pending={mutation.pending}
        >
          This permanently removes the project, all its tasks, comments and
          assignments.
        </Confirm>
      )}
    </>
  );
}
export default ProjectDetail;
