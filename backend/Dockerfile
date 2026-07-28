# Build context is expected to be the repository root (see docker-compose.yml).

FROM node:20-alpine AS frontend-build
# e.g. "/language-cards" when served behind a reverse-proxy path prefix, empty for root.
ARG CONTEXT_PATH=
WORKDIR /frontend
COPY frontend/package*.json ./
RUN npm ci
COPY frontend/ ./
RUN VITE_BASE_PATH="${CONTEXT_PATH}/" npm run build

FROM maven:3.9-eclipse-temurin-21 AS backend-build
WORKDIR /backend
COPY backend/pom.xml ./
RUN mvn -B dependency:go-offline
COPY backend/src ./src
COPY --from=frontend-build /frontend/dist ./src/main/resources/static
RUN mvn -B package -DskipTests

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=backend-build /backend/target/backend.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
