# Stage 1: Build frontend
FROM node:20-alpine AS frontend
WORKDIR /app/frontend
COPY frontend/package*.json ./
RUN npm ci
COPY frontend/ .
RUN npm run build

# Stage 2: Build Spring Boot JAR
FROM eclipse-temurin:21 AS builder
WORKDIR /app
COPY . .
COPY --from=frontend /app/frontend/dist src/main/resources/static/admin
RUN ./gradlew bootJar -x frontendBuild -x test --no-daemon

# Stage 3: Runtime image
FROM eclipse-temurin:21-jre AS runtime
WORKDIR /app
COPY --from=builder /app/build/libs/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
