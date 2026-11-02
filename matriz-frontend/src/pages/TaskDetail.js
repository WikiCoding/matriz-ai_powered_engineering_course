import { useEffect, useState } from "react";
import { Link, useNavigate, useParams } from "react-router-dom";
import { projectPath, assigneeOptionsPath } from "../api/projects";
import {
  taskPath,
  assignmentsPath,
  commentsPath,
  updateTask,
  assignTask,
  unassignTask,
  createComment,
  deleteTask,
} from "../api/tasks";
import { useData } from "../hooks/useData";
import { useMutation } from "../hooks/useMutation";
import Message from "../components/Message";
import TaskFields from "../components/TaskFields";
import Badge from "../components/Badge";
import Comment from "../components/Comment";
import Confirm from "../components/Confirm";
import Section from "../components/Section";
import Field from "../components/Field";
import { newTask } from "../constants";

function TaskDetail({ token, role }) {
  const { taskId } = useParams(),
    navigate = useNavigate(),
    manager = role === "PROJECT_MANAGER",
    engineer = role === "SOFTWARE_ENGINEER";
  const [version, setVersion] = useState(0),
    [form, setForm] = useState(newTask),
    [comment, setComment] = useState(""),
    [assignee, setAssignee] = useState(""),
    [confirm, setConfirm] = useState(false);
  const task = useData(taskPath(taskId), token, version),
    project = useData(
      task.data ? projectPath(task.data.projectId) : null,
      token,
    ),
    assignments = useData(assignmentsPath(taskId), token, version),
    comments = useData(commentsPath(taskId), token, version),
    options = useData(
      manager && task.data ? assigneeOptionsPath(task.data.projectId) : null,
      token,
    );
  const mutation = useMutation();
  useEffect(() => {
    if (task.data)
      setForm({
        title: task.data.title,
        description: task.data.description,
        effort: String(task.data.effort),
        status: task.data.status,
        priority: task.data.priority,
      });
  }, [task.data]);
  async function save(e) {
    e.preventDefault();
    if (
      await mutation.run(() =>
        updateTask(token, taskId, { ...form, effort: Number(form.effort) }),
      )
    )
      setVersion((v) => v + 1);
  }
  async function assign() {
    if (
      await mutation.run(
        () => assignTask(token, taskId, assignee),
        "Engineer assigned.",
      )
    ) {
      setAssignee("");
      setVersion((v) => v + 1);
    }
  }
  async function unassign() {
    if (
      await mutation.run(
        () => unassignTask(token, taskId, assignments.data[0].userId),
        "Assignment removed.",
      )
    )
      setVersion((v) => v + 1);
  }
  async function post(e) {
    e.preventDefault();
    if (
      await mutation.run(
        () => createComment(token, taskId, comment),
        "Comment added.",
      )
    ) {
      setComment("");
      setVersion((v) => v + 1);
    }
  }
  async function remove() {
    if (await mutation.run(() => deleteTask(token, taskId)))
      navigate(projectPath(task.data.projectId));
  }
  return (
    <>
      <Link
        className="back"
        to={task.data ? projectPath(task.data.projectId) : "/projects"}
      >
        ← {project.data?.name || "Project"}
      </Link>
      <Message
        error={
          task.error ||
          project.error ||
          assignments.error ||
          comments.error ||
          options.error ||
          mutation.error
        }
        success={mutation.success}
      />
      {task.loading ? (
        <p role="status">Loading…</p>
      ) : (
        task.data && (
          <>
            <div className="heading">
              <div>
                <p className="eyebrow">TASK DETAIL</p>
                <h1>{task.data.title}</h1>
              </div>
              {manager && (
                <button
                  className="danger-outline"
                  onClick={() => setConfirm(true)}
                >
                  Delete task
                </button>
              )}
            </div>
            <Section title="Details">
              {manager ? (
                <form onSubmit={save}>
                  <TaskFields value={form} change={setForm} />
                  <div className="actions">
                    <button className="primary" disabled={mutation.pending}>
                      Save changes
                    </button>
                  </div>
                </form>
              ) : (
                <>
                  <p>{task.data.description}</p>
                  <div className="badges">
                    <Badge value={task.data.status} />
                    <Badge value={task.data.priority} />
                    <span>Effort {task.data.effort}</span>
                  </div>
                </>
              )}
              <div className="meta">
                <div>
                  <small>Project</small>
                  <strong>{project.data?.name || "—"}</strong>
                </div>
                <div>
                  <small>Created</small>
                  <strong>{task.data.createdAt}</strong>
                </div>
                <div>
                  <small>Updated</small>
                  <strong>{task.data.updatedAt}</strong>
                </div>
              </div>
            </Section>
            <Section title="Assignee">
              {assignments.data?.length ? (
                <div className="assignment">
                  <span>
                    {options.data?.find(
                      (o) => o.id === assignments.data[0].userId,
                    )?.displayName || "Assigned engineer"}
                  </span>
                  {manager && (
                    <button disabled={mutation.pending} onClick={unassign}>
                      Remove
                    </button>
                  )}
                </div>
              ) : (
                <p className="muted">Unassigned</p>
              )}
              {manager && !assignments.data?.length && (
                <div className="inline">
                  <select
                    aria-label="Software Engineer"
                    value={assignee}
                    onChange={(e) => setAssignee(e.target.value)}
                  >
                    <option value="">Select a Software Engineer</option>
                    {options.data?.map((o) => (
                      <option key={o.id} value={o.id}>
                        {o.displayName}
                      </option>
                    ))}
                  </select>
                  <button
                    disabled={!assignee || mutation.pending}
                    onClick={assign}
                  >
                    Assign
                  </button>
                </div>
              )}
            </Section>
            <Section title="Comments">
              {comments.loading ? (
                <p role="status">Loading…</p>
              ) : comments.data?.length ? (
                comments.data.map((c) => (
                  <Comment key={c.id} value={c} token={token} />
                ))
              ) : (
                <p className="muted">No comments yet.</p>
              )}
              {engineer && (
                <form className="comment-form" onSubmit={post}>
                  <Field
                    label="Add a comment"
                    rows={3}
                    required
                    maxLength="2000"
                    value={comment}
                    onChange={setComment}
                  />
                  <button
                    className="primary"
                    disabled={mutation.pending || !comment.trim()}
                  >
                    Post comment
                  </button>
                </form>
              )}
            </Section>
          </>
        )
      )}
      {confirm && (
        <Confirm
          title="Delete task?"
          close={() => setConfirm(false)}
          submit={remove}
          pending={mutation.pending}
        >
          This permanently removes the task, its comments and assignment.
        </Confirm>
      )}
    </>
  );
}
export default TaskDetail;
