import { useEffect, useRef, useState } from "react";
import { api } from "../api/client";

function useData(path, token, version = 0) {
  const request = useRef(null);
  const [result, setResult] = useState({
    loading: true,
    data: null,
    error: "",
  });
  useEffect(() => {
    if (!path) {
      request.current = null;
      setResult({ loading: false, data: null, error: "" });
      return;
    }
    let active = true;
    setResult((old) => ({ ...old, loading: true, error: "" }));
    if (
      request.current?.path !== path ||
      request.current?.token !== token ||
      request.current?.version !== version
    ) {
      request.current = { path, token, version, promise: api(path, token) };
    }
    request.current.promise
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
