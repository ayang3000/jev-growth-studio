FROM gradle:8.14.3-jdk17 AS build
WORKDIR /workspace
COPY --chown=gradle:gradle build.gradle settings.gradle gradle.properties ./
RUN gradle dependencies --no-daemon
COPY --chown=gradle:gradle src src
RUN gradle clean test bootJar --no-daemon

FROM eclipse-temurin:17-jre
WORKDIR /app
RUN addgroup --system spring && adduser --system --ingroup spring spring
COPY --from=build /workspace/build/libs/jev-growth-studio-*.jar app.jar
RUN mkdir -p /app/data && chown -R spring:spring /app
USER spring
EXPOSE 8080
HEALTHCHECK --interval=30s --timeout=3s --start-period=25s --retries=3 \
  CMD wget -qO- http://localhost:8080/actuator/health || exit 1
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
