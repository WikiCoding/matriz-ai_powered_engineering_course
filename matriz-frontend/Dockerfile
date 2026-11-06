FROM node:22-bookworm-slim

WORKDIR /app
COPY matriz-frontend/package.json matriz-frontend/package-lock.json ./
RUN npm ci && chown -R node:node /app

COPY --chown=node:node matriz-frontend/public/ ./public/
COPY --chown=node:node matriz-frontend/src/ ./src/

ENV HOST=0.0.0.0
ENV PORT=3000
ENV BROWSER=none
ENV FRONTEND_DEV_PROXY_TARGET=http://backend:8080

USER node
EXPOSE 3000
CMD ["npm", "start"]
