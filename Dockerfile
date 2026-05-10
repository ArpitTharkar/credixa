# syntax=docker/dockerfile:1

FROM eclipse-temurin:17-jdk AS build
WORKDIR /workspace

# Ensure the Android app module is not included during CI/CD backend builds.
ENV RENDER=true

COPY gradlew gradlew
COPY gradle gradle
COPY settings.gradle.kts build.gradle.kts gradle.properties ./
COPY backend backend

RUN sed -i 's/\r$//' gradlew \
	&& sed -i '/^org\.gradle\.java\.home=/d' gradle.properties \
	&& chmod +x gradlew \
	&& ./gradlew :backend:bootJar --no-daemon

FROM eclipse-temurin:17-jre
WORKDIR /app

# Copy the entrypoint script
COPY backend/docker-entrypoint.sh /app/entrypoint.sh
RUN chmod +x /app/entrypoint.sh

# Copy the built Spring Boot JAR
COPY --from=build /workspace/backend/build/libs/*.jar /app/app.jar

EXPOSE 8080

# Use startup script to validate env vars and start Spring Boot
ENTRYPOINT ["/app/entrypoint.sh"]
