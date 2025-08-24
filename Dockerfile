# ---------- Build stage ----------
FROM eclipse-temurin:17-jdk AS build
WORKDIR /workspace

# Copy sources
COPY . .

# Ensure Gradle wrapper is executable and has LF line endings
RUN chmod +x ./gradlew && sed -i 's/\r$//' ./gradlew

# Build only the server distribution
RUN ./gradlew --no-daemon :server:installDist

# ---------- Runtime stage ----------
FROM eclipse-temurin:17-jre
WORKDIR /app

# Copy server distribution from build stage
COPY --from=build /workspace/server/build/install/server /app

# Render provides PORT; default to 8080 for local
ENV PORT=8080
ENV JAVA_OPTS=""

EXPOSE 8080

# Health endpoint is /health (configured in code)

# Start the Ktor server distribution script
CMD ["/app/bin/server"]
