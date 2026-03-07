# Build stage
FROM gradle:8.5.0-jdk21 AS build
WORKDIR /app
COPY . .
# We assume the context is the whole monorepo, so we can run gradlew
# RUN ./gradlew :modules:backend:api:shadowJar # Example of building fat jar
RUN ./gradlew :modules:backend:api:build -x test

# Run stage
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
# We need to copy the built jar from the build stage. 
# Typical location is build/libs/api-all.jar. Let's assume Ktor standard fat jar plugin
COPY --from=build /app/modules/backend/api/build/libs/*-all.jar /app/app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
