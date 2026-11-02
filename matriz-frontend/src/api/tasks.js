import { api } from "./client";

export const taskPath = (id) => `/tasks/${id}`;
export const assignmentsPath = (id) => `${taskPath(id)}/assignments`;
export const commentsPath = (id) => `${taskPath(id)}/comments`;
export const createTask = (token, value) => api("/tasks", token, "POST", value);
export const updateTask = (token, id, value) =>
  api(taskPath(id), token, "PATCH", value);
export const deleteTask = (token, id) => api(taskPath(id), token, "DELETE");
export const assignTask = (token, id, userId) =>
  api(assignmentsPath(id), token, "POST", { userId });
export const unassignTask = (token, id, userId) =>
  api(`${assignmentsPath(id)}/${userId}`, token, "DELETE");
export const createComment = (token, taskId, content) =>
  api("/comments", token, "POST", { taskId, content });
