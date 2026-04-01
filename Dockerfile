# Multi-stage build for did-server SpringBoot application
# Build stage
FROM maven:3.9-eclipse-temurin-21-alpine AS builder

WORKDIR /app

# Copy pom files first for better layer caching
# Copy all pom.xml files to satisfy Maven module structure
COPY pom.xml .
COPY did-toolbox/pom.xml did-toolbox/
COPY did-server/pom.xml did-server/

# Copy examples pom files (needed for module structure, but we won't build them)
COPY did-toolbox/examples/pom.xml did-toolbox/examples/
COPY did-toolbox/examples/did-operations/pom.xml did-toolbox/examples/did-operations/
COPY did-toolbox/examples/using-pre-rotation-keys/pom.xml did-toolbox/examples/using-pre-rotation-keys/

# Download dependencies (cached layer) - only for modules we need
RUN mvn dependency:go-offline -B -pl did-server -am

# Copy source code
COPY did-toolbox/src did-toolbox/src
COPY did-server/src did-server/src

# Build the did-server module (which includes did-toolbox as dependency)
RUN mvn clean package -B -DskipTests -pl did-server -am

# Runtime stage - using distroless for minimal image
FROM gcr.io/distroless/java21-debian12

WORKDIR /app

# Copy the built JAR from builder stage
COPY --from=builder /app/did-server/target/did-server-*.jar app.jar

# Accept timezone as a build argument with a default
ARG TZ="Europe/Zurich"
ENV TZ=${TZ}

# Expose the default SpringBoot port
EXPOSE 8080

# Run the SpringBoot application
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
