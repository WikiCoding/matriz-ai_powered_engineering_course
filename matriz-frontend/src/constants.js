export const statuses = ["TODO", "IN_PROGRESS", "DONE"];
export const priorities = ["LOW", "MEDIUM", "HIGH"];
export const newTask = {
  title: "",
  description: "",
  status: "TODO",
  priority: "MEDIUM",
  effort: "3",
};
export const newProject = { name: "", description: "", teamId: "" };
export const choices = (values) =>
  values.map((value) => [value, value.replace("_", " ")]);
