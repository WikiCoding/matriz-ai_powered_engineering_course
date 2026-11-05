import { act, renderHook } from '@testing-library/react';
import { useMutation } from './useMutation';

test('blocks overlapping submissions and allows retry after failure', async () => {
  // Arrange
  const { result } = renderHook(() => useMutation());
  let reject;
  const work = jest.fn(() => new Promise((resolve, fail) => { reject = fail; }));
  let first, duplicate;

  // Act
  act(() => {
    first = result.current.run(work);
    duplicate = result.current.run(work);
  });

  // Assert
  expect(await duplicate).toBe(false);
  expect(work).toHaveBeenCalledTimes(1);
  expect(result.current.pending).toBe(true);

  // Act
  await act(async () => { reject(new Error('Unavailable')); await first; });

  // Assert
  expect(await first).toBe(false);
  expect(result.current.error).toBe('Unavailable');
  expect(result.current.pending).toBe(false);

  // Act
  await act(async () => { await result.current.run(async () => {}, 'Saved'); });

  // Assert
  expect(result.current.success).toBe('Saved');
  expect(result.current.error).toBe('');
  expect(result.current.pending).toBe(false);
});
