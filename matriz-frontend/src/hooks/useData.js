import { useEffect, useState } from "react";
import { api } from "../api/client";

function useData(path, token, version = 0) {
  const [result, setResult] = useState({
    loading: true,
    data: null,
    error: "",
  });
  useEffect(() => {
    if (!path) return;
    let active = true;
    setResult((old) => ({ ...old, loading: true, error: "" }));
    api(path, token)
      .then((data) => active && setResult({ loading: false, data, error: "" }))
      .catch(
        (error) =>
          active &&
          setResult({ loading: false, data: null, error: error.message }),
      );
    return () => {
      active = false;
    };
  }, [path, token, version]);
  return result;
}
export { useData };
