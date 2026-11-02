import { api } from "./client";

export const usersPath = (page) => `/users?page=${page}&size=10`;
export const userDisplayPath = (id) => `/users/${id}/display`;
export const createUser = (token, value) => api("/users", token, "POST", value);
