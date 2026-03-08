# Build stage — JDK 25 required by jvmToolchain(25)
FROM eclipse-temurin:25-jdk AS build
WORKDIR /app
COPY . .
RUN ./gradlew :modules:backend:api:build -x test --no-daemon

# Run stage
FROM eclipse-temurin:25-jre
WORKDIR /app
COPY --from=build /app/modules/backend/api/build/libs/*-all.jar /app/app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
