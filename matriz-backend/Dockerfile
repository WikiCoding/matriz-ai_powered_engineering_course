FROM node:22-bookworm-slim AS node

FROM eclipse-temurin:21.0.8_9-jdk AS build
COPY --from=node /usr/local/bin/node /usr/local/bin/node
COPY --from=node /usr/local/lib/node_modules /usr/local/lib/node_modules
RUN ln -s /usr/local/lib/node_modules/npm/bin/npm-cli.js /usr/local/bin/npm
WORKDIR /workspace
COPY matriz-backend/ matriz-backend/
COPY matriz-frontend/ matriz-frontend/
WORKDIR /workspace/matriz-backend
RUN chmod +x mvnw && ./mvnw -B -DskipTests package

FROM eclipse-temurin:21.0.8_9-jre
WORKDIR /app
COPY --from=build /workspace/matriz-backend/target/matriz-backend-0.0.1-SNAPSHOT.jar app.jar
USER 10001
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
