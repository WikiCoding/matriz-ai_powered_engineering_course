import { fireEvent, render, screen } from '@testing-library/react';
import Confirm from './Confirm';

test('contains keyboard focus, cancels on Escape and restores focus', () => {
  // Arrange
  const trigger = document.createElement('button');
  document.body.appendChild(trigger);
  trigger.focus();
  const close = jest.fn();

  // Act
  const { unmount } = render(
    <Confirm title="Delete task?" close={close} submit={jest.fn()}>
      Permanent removal.
    </Confirm>,
  );
  const cancel = screen.getByRole('button', { name: 'Cancel' });
  const remove = screen.getByRole('button', { name: 'Delete' });

  // Assert
  expect(cancel).toHaveFocus();

  // Act
  fireEvent.keyDown(cancel, { key: 'Tab', shiftKey: true });

  // Assert
  expect(remove).toHaveFocus();

  // Act
  fireEvent.keyDown(remove, { key: 'Tab' });

  // Assert
  expect(cancel).toHaveFocus();

  // Act
  fireEvent.keyDown(cancel, { key: 'Escape' });
  unmount();

  // Assert
  expect(close).toHaveBeenCalledTimes(1);
  expect(trigger).toHaveFocus();
  trigger.remove();
});

test('keeps focus inside while pending and displays deletion failure inside the dialog', () => {
  // Arrange
  const close = jest.fn();
  const submit = jest.fn();
  const { rerender } = render(<Confirm title="Delete task?" close={close} submit={submit} />);

  // Act
  rerender(<Confirm title="Delete task?" close={close} submit={submit} pending />);
  fireEvent.keyDown(screen.getByRole('dialog'), { key: 'Escape' });
  fireEvent.keyDown(screen.getByRole('dialog'), { key: 'Tab' });

  // Assert
  expect(screen.getByRole('dialog')).toHaveFocus();
  expect(screen.getByRole('button', { name: 'Cancel' })).toBeDisabled();
  expect(screen.getByRole('button', { name: 'Deleting…' })).toBeDisabled();
  expect(close).not.toHaveBeenCalled();

  // Act
  rerender(<Confirm title="Delete task?" close={close} submit={submit} error="Deletion failed" />);
  fireEvent.click(screen.getByRole('button', { name: 'Delete' }));

  // Assert
  expect(screen.getByRole('button', { name: 'Cancel' })).toHaveFocus();
  expect(screen.getByRole('dialog')).toContainElement(screen.getByRole('alert'));
  expect(screen.getByRole('alert')).toHaveTextContent('Deletion failed');
  expect(submit).toHaveBeenCalledTimes(1);
});
