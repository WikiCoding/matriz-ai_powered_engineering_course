import { api } from "./client";

export const teamsPath = (page) => `/teams?page=${page}&size=10`;
export const teamOptionsPath = "/teams?size=100";
export const createTeam = (token, name) =>
  api("/teams", token, "POST", { name });
