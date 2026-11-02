function Confirm({ title, children, close, submit, pending }) {
  return (
    <div className="backdrop">
      <div
        className="dialog"
        role="dialog"
        aria-modal="true"
        aria-label={title}
      >
        <h2>{title}</h2>
        <p>{children}</p>
        <div className="actions">
          <button disabled={pending} onClick={close}>
            Cancel
          </button>
          <button className="danger" disabled={pending} onClick={submit}>
            {pending ? "Deleting…" : "Delete"}
          </button>
        </div>
      </div>
    </div>
  );
}
export default Confirm;
