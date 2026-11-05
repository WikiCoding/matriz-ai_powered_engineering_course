import { useRef, useState } from "react";

function useMutation() {
  const inFlight = useRef(false);
  const [pending, setPending] = useState(false),
    [error, setError] = useState(""),
    [success, setSuccess] = useState("");
  async function run(work, message) {
    if (inFlight.current) return false;
    inFlight.current = true;
    setPending(true);
    setError("");
    setSuccess("");
    try {
      await work();
      setSuccess(message || "Changes saved.");
      return true;
    } catch (failure) {
      setError(failure.message);
      return false;
    } finally {
      inFlight.current = false;
      setPending(false);
    }
  }
  return { pending, error, success, run };
}
export { useMutation };
