FROM eclipse-temurin:21.0.8_9-jre
WORKDIR /app
# Build with ./mvnw verify before docker compose --profile prod up --build.
COPY target/matriz-backend-0.0.1-SNAPSHOT.jar app.jar
USER 10001
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
