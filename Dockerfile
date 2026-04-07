# Stage 1: Build the React Frontend
FROM node:22-alpine AS frontend-builder
WORKDIR /app
COPY frontend/package*.json ./
RUN npm install
COPY frontend/ ./
# Pass Auth0 variables as build args to bake into Vite app
ARG VITE_AUTH0_DOMAIN
ARG VITE_AUTH0_CLIENT_ID
ARG VITE_AUTH0_AUDIENCE
ARG VITE_API_BASE_URL
ENV VITE_AUTH0_DOMAIN=$VITE_AUTH0_DOMAIN
ENV VITE_AUTH0_CLIENT_ID=$VITE_AUTH0_CLIENT_ID
ENV VITE_AUTH0_AUDIENCE=$VITE_AUTH0_AUDIENCE
ENV VITE_API_BASE_URL=$VITE_API_BASE_URL
RUN npm run build

# Stage 2: Build the Spring Boot Backend
FROM maven:3.8.5-openjdk-17-slim AS backend-builder
WORKDIR /app
COPY expense-splitter/pom.xml ./
RUN mvn dependency:go-offline -B
COPY expense-splitter/src ./src
# Copy the compiled React assets into static resources
COPY --from=frontend-builder /app/dist ./src/main/resources/static
RUN mvn package -DskipTests

# Stage 3: The Runtime Image
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=backend-builder /app/target/expense-splitter-*.jar app.jar

# Cloud Deployment Settings
ENV SERVER_PORT=8080
ENV SPRING_PROFILES_ACTIVE=prod
ENV JAVA_TOOL_OPTIONS="-Djava.awt.headless=true"

# Data persistence for H2 (if not using Supabase)
RUN mkdir -p /app/data
VOLUME /app/data

EXPOSE 8080
ENTRYPOINT ["java", "-cp", "app.jar", "-Dloader.main=com.malcolm.expensesplitter.WebSplitterApplication", "org.springframework.boot.loader.launch.JarLauncher"]
# Alternative for simplicity:
# ENTRYPOINT ["java", "-jar", "app.jar"]
