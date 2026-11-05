import { api } from "./client";

export const getProfile = (token) => api("/users/me", token);
export const login = (username, password) =>
  api("/auth/login", null, "POST", { username, password });
export const logout = (token, refreshToken) =>
  api("/auth/logout", token, "POST", { refresh_token: refreshToken });
