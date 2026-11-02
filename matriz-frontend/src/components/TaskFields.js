import Field from "./Field";
import { statuses, priorities, choices } from "../constants";

function TaskFields({ value, change }) {
  const set = (key, v) => change({ ...value, [key]: v });
  return (
    <div className="form-grid">
      <Field
        label="Title"
        required
        minLength="3"
        maxLength="150"
        value={value.title}
        onChange={(v) => set("title", v)}
      />
      <Field
        label="Status"
        value={value.status}
        onChange={(v) => set("status", v)}
        options={choices(statuses)}
      />
      <Field
        label="Description"
        rows={4}
        maxLength="2000"
        value={value.description}
        onChange={(v) => set("description", v)}
      />
      <div className="stack">
        <Field
          label="Priority"
          value={value.priority}
          onChange={(v) => set("priority", v)}
          options={choices(priorities)}
        />
        <Field
          label="Effort"
          value={String(value.effort)}
          onChange={(v) => set("effort", v)}
          options={[1, 2, 3, 5, 8, 13].map((n) => [String(n), String(n)])}
        />
      </div>
    </div>
  );
}
export default TaskFields;
