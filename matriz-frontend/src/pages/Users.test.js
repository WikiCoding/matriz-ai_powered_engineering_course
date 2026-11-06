import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import Users from './Users';

beforeEach(() => { global.fetch = jest.fn(); });
afterEach(() => jest.resetAllMocks());

test.each([false, true])('creates a user using only email as identity; API fails: %s', async (fails) => {
  // Arrange
  fetch.mockImplementation((url, options) => {
    if (options.method === 'POST') return Promise.resolve(fails
      ? { ok: false, status: 409, json: async () => ({ message: 'Email already exists.' }) }
      : { ok: true, status: 201, json: async () => ({ email: 'new@example.com' }) });
    return Promise.resolve({ ok: true, status: 200, json: async () => ({
      content: url.includes('/teams') ? [{ id: 'team-1', name: 'Atlas' }] : [], totalPages: 0,
    }) });
  });
  render(<Users token="admin-token" />);
  await screen.findByRole('option', { name: 'Atlas' });
  fireEvent.change(screen.getByLabelText('First name'), { target: { value: 'New' } });
  fireEvent.change(screen.getByLabelText('Last name'), { target: { value: 'User' } });
  fireEvent.change(screen.getByLabelText('Email'), { target: { value: 'new@example.com' } });
  fireEvent.change(screen.getByLabelText('Password'), { target: { value: 'test-password' } });
  fireEvent.change(screen.getByLabelText('Team'), { target: { value: 'team-1' } });

  // Act
  fireEvent.click(screen.getByRole('button', { name: 'Create user' }));
  await waitFor(() => expect(screen.getByRole('button', { name: 'Create user' })).toBeEnabled());

  // Assert
  expect(screen.queryByLabelText('Username')).not.toBeInTheDocument();
  expect(fetch).toHaveBeenCalledWith(expect.stringContaining('/users'), expect.objectContaining({
    method: 'POST', body: JSON.stringify({ email: 'new@example.com', firstName: 'New', lastName: 'User',
      password: 'test-password', role: 'SOFTWARE_ENGINEER', teamId: 'team-1' }),
  }));
  if (fails) {
    expect(screen.getByRole('alert')).toHaveTextContent('Email already exists.');
    expect(screen.getByLabelText('Email')).toHaveValue('new@example.com');
    expect(screen.getByLabelText('Password')).toHaveValue('test-password');
  } else {
    expect(screen.getByRole('status')).toHaveTextContent('User created.');
    expect(screen.getByLabelText('Email')).toHaveValue('');
  }
});
