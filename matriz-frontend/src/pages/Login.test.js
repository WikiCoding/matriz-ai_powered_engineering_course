import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import Login from './Login';

test('accepts a non-email username and retains credentials on failure', async () => {
  // Arrange
  const login = jest.fn().mockRejectedValue(new Error('Login unavailable'));
  render(<Login login={login} />);
  const username = screen.getByLabelText('Username');
  const password = screen.getByLabelText('Password');

  // Act
  fireEvent.change(username, { target: { value: 'alex.manager' } });
  fireEvent.change(password, { target: { value: 'test-password' } });
  fireEvent.click(screen.getByRole('button', { name: 'Sign in' }));
  await waitFor(() => expect(screen.getByRole('alert')).toHaveTextContent('Login unavailable'));

  // Assert
  expect(username).toHaveAttribute('type', 'text');
  expect(username).toBeValid();
  expect(login).toHaveBeenCalledWith('alex.manager', 'test-password');
  expect(username).toHaveValue('alex.manager');
  expect(password).toHaveValue('test-password');
  expect(screen.getByRole('button', { name: 'Sign in' })).toBeEnabled();
});
