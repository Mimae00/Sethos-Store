# syntax=docker/dockerfile:1
# ---------------------------------------------------------------------------
# Sethos Store backend image.
#
#   docker build -t sethos-store-backend .
#
# Three stages:
#   build    compiles the jar with the project's own Maven wrapper
#   extract  splits the jar into Spring Boot layers
#   runtime  a JRE-only image that runs as a non-root user
# ---------------------------------------------------------------------------

# --- build ------------------------------------------------------------------
FROM eclipse-temurin:17.0.20.1_1-jdk-alpine-3.24 AS build
WORKDIR /workspace

# Wrapper and pom first: dependency resolution is the slow part, and this layer is
# only invalidated when the pom changes, not on every source edit.
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
# Guard against CRLF line endings from a Windows checkout, which break the shell script.
RUN sed -i 's/\r$//' mvnw && chmod +x mvnw
RUN --mount=type=cache,target=/root/.m2 ./mvnw -B -q dependency:go-offline

COPY src/ src/
# Tests are left to CI so an image build is not blocked by, or slowed down by, them.
RUN --mount=type=cache,target=/root/.m2 ./mvnw -B -q package -DskipTests \
    && cp target/sethos-store-backend-*.jar app.jar

# --- extract ----------------------------------------------------------------
FROM eclipse-temurin:17.0.20.1_1-jdk-alpine-3.24 AS extract
WORKDIR /workspace
COPY --from=build /workspace/app.jar app.jar
# Dependencies change far less often than application code. Splitting them into their
# own layers means a code change pushes a few hundred KB, not the whole ~60 MB jar.
RUN java -Djarmode=tools -jar app.jar extract --layers --launcher --destination extracted

# --- runtime ----------------------------------------------------------------
FROM eclipse-temurin:17.0.20.1_1-jre-alpine-3.24 AS runtime

LABEL org.opencontainers.image.title="sethos-store-backend" \
      org.opencontainers.image.description="Sethos Store point of sale REST API"

RUN addgroup -S sethos && adduser -S -G sethos -h /app sethos
WORKDIR /app

# Least-changing first, so a rebuild reuses as many cached layers as possible.
COPY --from=extract --chown=sethos:sethos /workspace/extracted/dependencies/ ./
COPY --from=extract --chown=sethos:sethos /workspace/extracted/spring-boot-loader/ ./
COPY --from=extract --chown=sethos:sethos /workspace/extracted/snapshot-dependencies/ ./
COPY --from=extract --chown=sethos:sethos /workspace/extracted/application/ ./

USER sethos

# Size the heap from the container's memory limit rather than the host's RAM, and exit
# on OOM so the orchestrator restarts a clean process instead of a wedged one.
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75 -XX:+ExitOnOutOfMemoryError"

EXPOSE 8080

# busybox wget ships with Alpine, so no extra package is needed for the probe.
HEALTHCHECK --interval=15s --timeout=5s --start-period=60s --retries=5 \
    CMD wget -q -O /dev/null http://127.0.0.1:8080/actuator/health || exit 1

# Exec form so the JVM is PID 1 and receives SIGTERM for a graceful shutdown.
ENTRYPOINT ["java", "org.springframework.boot.loader.launch.JarLauncher"]
