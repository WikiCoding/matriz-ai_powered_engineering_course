const { createProxyMiddleware } = require("http-proxy-middleware");

module.exports = function setupProxy(app) {
  app.use(
    "/api/v1",
    createProxyMiddleware({
      target: process.env.FRONTEND_DEV_PROXY_TARGET || "http://localhost:8080",
      changeOrigin: true,
      xfwd: true,
    }),
  );
};
