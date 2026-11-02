import { api } from "./client";

export const projectsPath = (page) => `/projects?page=${page}&size=10`;
export const projectPath = (id) => `/projects/${id}`;
export const projectTasksPath = (id, page, status, priority) =>
  `${projectPath(id)}/tasks?page=${page}&size=10${status ? `&status=${status}` : ""}${priority ? `&priority=${priority}` : ""}`;
export const assigneeOptionsPath = (id) =>
  `${projectPath(id)}/assignee-options`;
export const createProject = (token, value) =>
  api("/projects", token, "POST", value);
export const updateProject = (token, id, value) =>
  api(projectPath(id), token, "PUT", value);
export const deleteProject = (token, id) =>
  api(projectPath(id), token, "DELETE");
