FROM eclipse-temurin:21.0.8_9-jdk AS build
WORKDIR /workspace
COPY matriz-backend/ matriz-backend/
WORKDIR /workspace/matriz-backend
RUN chmod +x mvnw && ./mvnw -B -DskipTests package

FROM eclipse-temurin:21.0.8_9-jre
WORKDIR /app
COPY --from=build /workspace/matriz-backend/target/matriz-backend-0.0.1-SNAPSHOT.jar app.jar
USER 10001
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
