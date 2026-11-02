import { useState } from "react";

function useMutation() {
  const [pending, setPending] = useState(false),
    [error, setError] = useState(""),
    [success, setSuccess] = useState("");
  async function run(work, message) {
    if (pending) return false;
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
      setPending(false);
    }
  }
  return { pending, error, success, run };
}
export { useMutation };
