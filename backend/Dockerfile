FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
COPY checkstyle.xml .
RUN mvn -B -q dependency:go-offline
COPY src ./src
RUN mvn -B -q package -DskipTests

FROM eclipse-temurin:21-jre-alpine AS run
WORKDIR /app
RUN addgroup -S app && adduser -S app -G app && mkdir -p /app/exports && chown app:app /app/exports
COPY --from=build --chown=app:app /app/target/*.jar app.jar
USER app

# Hosts like Render inject $PORT (application.properties reads it, default 8080).
EXPOSE 8080
# Size the heap from the container's memory limit instead of the host's, so small instances
# (512MB) are not OOM-killed. Override with JAVA_OPTS if needed.
ENV JAVA_OPTS="-XX:MaxRAMPercentage=70 -XX:+ExitOnOutOfMemoryError"
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar app.jar"]
