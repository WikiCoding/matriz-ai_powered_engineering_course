import { renderHook, waitFor } from '@testing-library/react';
import { useData } from './useData';
import { api } from '../api/client';

jest.mock('../api/client');

test('clears dependent data when its request is disabled', async () => {
  // Arrange
  api.mockResolvedValue({ displayName: 'Engineer' });
  const { result, rerender } = renderHook(({ path }) => useData(path, 'token'), {
    initialProps: { path: '/users/person/display' },
  });
  await waitFor(() => expect(result.current.data).toEqual({ displayName: 'Engineer' }));

  // Act
  rerender({ path: null });

  // Assert
  expect(result.current).toEqual({ loading: false, data: null, error: '' });
  expect(api).toHaveBeenCalledTimes(1);
});
