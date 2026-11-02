import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import App from './App';

beforeEach(() => { sessionStorage.clear(); window.history.pushState({}, '', '/'); global.fetch = jest.fn(); });
afterEach(() => jest.resetAllMocks());

test('signs in and shows accessible project counts', async () => {
  // Arrange
  fetch.mockImplementation((url) => Promise.resolve({ ok: true, json: async () => url.includes('/auth/login') ? { access_token: 'token', refresh_token: 'refresh' } : url.includes('/users/me') ? { displayName: 'Alex', email: 'alex@example.com', role: 'PROJECT_MANAGER' } : { projects: [], taskCounts: { TODO: 2, IN_PROGRESS: 1, DONE: 1 } } }));
  render(<App />);

  // Act
  fireEvent.change(screen.getByLabelText('Email'), { target: { value: 'alex@example.com' } });
  fireEvent.change(screen.getByLabelText('Password'), { target: { value: 'password' } });
  fireEvent.click(screen.getByRole('button', { name: 'Sign in' }));

  // Assert
  expect(await screen.findByText('Welcome, Alex')).toBeInTheDocument();
  await waitFor(() => expect(screen.getByText('2')).toBeInTheDocument());
  expect(fetch).toHaveBeenCalledWith(expect.stringContaining('/auth/login'), expect.objectContaining({ method: 'POST' }));
});

test('administrator home offers only administration navigation', async () => {
  // Arrange
  sessionStorage.setItem('matriz-session', JSON.stringify({ access_token: 'token' }));
  fetch.mockResolvedValue({ ok: true, json: async () => ({ displayName: 'Admin', role: 'ADMINISTRATOR' }) });
  render(<App />);

  // Act
  await screen.findByText('Welcome, Admin');

  // Assert
  expect(screen.getAllByRole('link', { name: 'Users' }).length).toBeGreaterThan(0);
  expect(screen.queryByRole('link', { name: 'Projects' })).not.toBeInTheDocument();
});

test('keeps a project form filled after the API rejects creation', async () => {
  // Arrange
  sessionStorage.setItem('matriz-session', JSON.stringify({ access_token: 'token' }));
  window.history.pushState({}, '', '/projects');
  fetch.mockImplementation((url, options) => {
    if (url.includes('/users/me')) return Promise.resolve({ ok: true, json: async () => ({ displayName: 'Alex', role: 'PROJECT_MANAGER' }) });
    if (url.includes('/teams')) return Promise.resolve({ ok: true, json: async () => ({ content: [{ id: 'team-1', name: 'Atlas' }] }) });
    if (options.method === 'POST') return Promise.resolve({ ok: false, status: 400, json: async () => ({ message: 'Name is invalid.' }) });
    return Promise.resolve({ ok: true, json: async () => ({ content: [], totalPages: 0 }) });
  });
  render(<App />);

  // Act
  fireEvent.click(await screen.findByRole('button', { name: '+ New project' }));
  fireEvent.change(screen.getByLabelText('Name'), { target: { value: 'Mobile App' } });
  fireEvent.change(screen.getByLabelText('Team'), { target: { value: 'team-1' } });
  fireEvent.click(screen.getByRole('button', { name: 'Save project' }));

  // Assert
  expect(await screen.findByRole('alert')).toHaveTextContent('Name is invalid.');
  expect(screen.getByLabelText('Name')).toHaveValue('Mobile App');
  expect(screen.getByLabelText('Team')).toHaveValue('team-1');
});

test('signs out and clears the stored session after provider confirmation', async () => {
  // Arrange
  sessionStorage.setItem('matriz-session', JSON.stringify({ access_token: 'token', refresh_token: 'refresh' }));
  fetch.mockImplementation((url) => Promise.resolve(url.includes('/auth/logout')
    ? { ok: true, status: 204 }
    : { ok: true, json: async () => ({ displayName: 'Admin', role: 'ADMINISTRATOR' }) }));
  render(<App />);

  // Act
  fireEvent.click(await screen.findByRole('button', { name: 'Sign out' }));

  // Assert
  expect(await screen.findByRole('button', { name: 'Sign in' })).toBeInTheDocument();
  expect(sessionStorage.getItem('matriz-session')).toBeNull();
  expect(fetch).toHaveBeenCalledWith(expect.stringContaining('/auth/logout'), expect.objectContaining({ method: 'POST' }));
});
