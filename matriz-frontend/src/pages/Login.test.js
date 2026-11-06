import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import Login from './Login';

test('accepts an email and retains credentials on failure', async () => {
  // Arrange
  const login = jest.fn().mockRejectedValue(new Error('Login unavailable'));
  render(<Login login={login} />);
  const email = screen.getByLabelText('Email');
  const password = screen.getByLabelText('Password');

  // Act
  fireEvent.change(email, { target: { value: 'alex.manager@example.com' } });
  fireEvent.change(password, { target: { value: 'test-password' } });
  fireEvent.click(screen.getByRole('button', { name: 'Sign in' }));
  await waitFor(() => expect(screen.getByRole('alert')).toHaveTextContent('Login unavailable'));

  // Assert
  expect(email).toHaveAttribute('type', 'email');
  expect(email).toBeValid();
  expect(login).toHaveBeenCalledWith('alex.manager@example.com', 'test-password');
  expect(email).toHaveValue('alex.manager@example.com');
  expect(password).toHaveValue('test-password');
  expect(screen.getByRole('button', { name: 'Sign in' })).toBeEnabled();
});

test.each(['', 'alex.manager'])('rejects an email without @: %s', (value) => {
  // Arrange
  const login = jest.fn();
  render(<Login login={login} />);
  const email = screen.getByLabelText('Email');
  fireEvent.change(email, { target: { value } });
  fireEvent.change(screen.getByLabelText('Password'), { target: { value: 'test-password' } });

  // Act
  fireEvent.click(screen.getByRole('button', { name: 'Sign in' }));

  // Assert
  expect(email).toBeInvalid();
  expect(login).not.toHaveBeenCalled();
});
