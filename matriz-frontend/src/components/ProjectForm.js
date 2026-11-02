import Field from "./Field";
import Section from "./Section";

function ProjectForm({ value, change, teams, submit, cancel, pending, title }) {
  return (
    <Section title={title}>
      <form onSubmit={submit}>
        <div className="form-grid">
          <Field
            label="Name"
            required
            minLength="3"
            maxLength="100"
            value={value.name}
            onChange={(v) => change({ ...value, name: v })}
          />
          <Field
            label="Team"
            required
            value={value.teamId}
            onChange={(v) => change({ ...value, teamId: v })}
            options={[
              ["", "Select a team"],
              ...(teams || []).map((t) => [t.id, t.name]),
            ]}
          />
          <Field
            label="Description"
            rows={4}
            maxLength="1000"
            value={value.description}
            onChange={(v) => change({ ...value, description: v })}
          />
        </div>
        <div className="actions">
          <button type="button" onClick={cancel}>
            Cancel
          </button>
          <button className="primary" disabled={pending}>
            {pending ? "Saving…" : "Save project"}
          </button>
        </div>
      </form>
    </Section>
  );
}
export default ProjectForm;
