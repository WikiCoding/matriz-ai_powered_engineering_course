const API = process.env.REACT_APP_API_BASE_URL || "";

export async function api(path, token, method = "GET", body) {
  const response = await fetch(`${API}/api/v1${path}`, {
    method,
    headers: {
      ...(token && { Authorization: `Bearer ${token}` }),
      ...(body && { "Content-Type": "application/json" }),
    },
    ...(body && { body: JSON.stringify(body) }),
  });
  if (!response.ok) {
    let detail;
    try {
      detail = await response.json();
    } catch (_) {
      /* empty response */
    }
    const error = new Error(
      detail?.message ||
        detail?.detail ||
        `Request failed (${response.status}).`,
    );
    error.status = response.status;
    throw error;
  }
  return response.status === 204 ? null : response.json();
}
