test('uses the configured backend origin for API requests', async () => {
  // Arrange
  const original = process.env.REACT_APP_API_BASE_URL;
  process.env.REACT_APP_API_BASE_URL = 'https://backend.example.test/';
  let api;
  jest.isolateModules(() => { ({ api } = require('./client')); });
  global.fetch = jest.fn().mockResolvedValue({ ok: true, status: 204 });

  try {
    // Act
    await api('/users/me', 'token');

    // Assert
    expect(fetch).toHaveBeenCalledWith('https://backend.example.test/api/v1/users/me',
      expect.objectContaining({ method: 'GET' }));
  } finally {
    if (original === undefined) delete process.env.REACT_APP_API_BASE_URL;
    else process.env.REACT_APP_API_BASE_URL = original;
  }
});
