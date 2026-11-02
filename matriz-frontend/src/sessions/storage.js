const key = "matriz-session";

export function readSession() {
  try {
    return JSON.parse(sessionStorage.getItem(key));
  } catch (_) {
    return null;
  }
}

export function saveSession(tokens) {
  sessionStorage.setItem(key, JSON.stringify(tokens));
}

export function clearSession() {
  sessionStorage.removeItem(key);
}
