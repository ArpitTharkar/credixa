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

COPY --from=build /workspace/backend/build/libs/*.jar /app/app.jar

EXPOSE 8080

ENTRYPOINT ["sh", "-c", "java -Dserver.port=${PORT:-8080} -jar /app/app.jar"]
