const { test, expect } = require('@playwright/test');
const { randomUUID } = require('node:crypto');

test('manager creates a task and sees it in the project', async ({ page, request }) => {
  // Arrange: provision a uniquely named disposable project in the isolated environment.
  const { E2E_EMAIL: email, E2E_PASSWORD: password, E2E_TEAM_ID: teamId } = process.env;
  if (!email || !password || !teamId) {
    throw new Error('Set E2E_EMAIL, E2E_PASSWORD and E2E_TEAM_ID for an isolated test environment.');
  }
  const api = (process.env.E2E_API_URL || 'http://localhost:8080').replace(/\/$/, '');
  const login = await request.post(`${api}/api/v1/auth/login`, { data: { email, password } });
  expect(login.status()).toBe(200);
  const { access_token: token } = await login.json();
  const headers = { Authorization: `Bearer ${token}` };
  const projectName = `E2E project ${randomUUID()}`;
  const title = `E2E task ${randomUUID()}`;
  const created = await request.post(`${api}/api/v1/projects`, {
    headers, data: { name: projectName, description: '', teamId },
  });
  expect(created.status()).toBe(201);
  const project = await created.json();
  try {
    // Act: authenticate through the UI, open the project and create a task.
    await page.goto('/');
    await page.getByLabel('Email').fill(email);
    await page.getByLabel('Password').fill(password);
    await page.getByRole('button', { name: 'Sign in' }).click();
    await page.getByRole('link', { name: new RegExp(projectName) }).click();
    await page.getByRole('button', { name: '+ New task' }).click();
    await page.getByLabel('Title').fill(title);
    await page.getByRole('button', { name: 'Create task' }).click();

    // Assert: the saved task appears in the project listing, including after a reload.
    await expect(page.getByRole('link', { name: title })).toBeVisible();
    await page.reload();
    await expect(page.getByRole('link', { name: title })).toBeVisible();
  } finally {
    const removed = await request.delete(`${api}/api/v1/projects/${project.id}`, { headers });
    expect(removed.status(), 'Disposable project cleanup').toBe(204);
  }
});
