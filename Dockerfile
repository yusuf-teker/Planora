# Use an official Gradle image to build the Ktor application
FROM gradle:8.7-jdk21 AS build
COPY --chown=gradle:gradle . /home/gradle/src
WORKDIR /home/gradle/src
RUN ./gradlew :server:installDist --no-daemon

# Use a lightweight JDK 21 image for the runtime
FROM eclipse-temurin:21-jre-alpine
EXPOSE 8080
WORKDIR /app

# Copy the built Ktor server from the build stage
COPY --from=build /home/gradle/src/server/build/install/server ./

# Render sets the PORT environment variable dynamically
ENV PORT=8080

# Run the Ktor server
CMD ["./bin/server"]
