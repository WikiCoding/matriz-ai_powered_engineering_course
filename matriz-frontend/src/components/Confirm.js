import { useEffect, useRef } from "react";

function Confirm({ title, children, close, submit, pending }) {
  const dialog = useRef(null);
  const cancel = useRef(null);
  useEffect(() => {
    const previous = document.activeElement;
    cancel.current.focus();
    return () => previous?.focus();
  }, []);
  function handleKeyDown(event) {
    if (event.key === "Escape") {
      event.preventDefault();
      if (!pending) close();
    }
    if (event.key === "Tab") {
      const buttons = [...dialog.current.querySelectorAll("button:not(:disabled)")];
      const first = buttons[0], last = buttons[buttons.length - 1];
      if (!first) {
        event.preventDefault();
      } else if (event.shiftKey && document.activeElement === first) {
        event.preventDefault();
        last.focus();
      } else if (!event.shiftKey && document.activeElement === last) {
        event.preventDefault();
        first.focus();
      }
    }
  }
  return (
    <div className="backdrop">
      <div
        ref={dialog}
        onKeyDown={handleKeyDown}
        className="dialog"
        role="dialog"
        aria-modal="true"
        aria-label={title}
      >
        <h2>{title}</h2>
        <p>{children}</p>
        <div className="actions">
          <button ref={cancel} disabled={pending} onClick={close}>
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
