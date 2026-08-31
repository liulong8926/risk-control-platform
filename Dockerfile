FROM gradle:8.14.3-jdk21 AS backend-build
WORKDIR /workspace
COPY settings.gradle build.gradle ./
COPY src ./src
RUN gradle --no-daemon bootJar

FROM eclipse-temurin:21-jre AS backend-runtime
WORKDIR /app
COPY --from=backend-build /workspace/build/libs/*.jar /app/enterprise-risk.jar
EXPOSE 8092
ENTRYPOINT ["java", "-jar", "/app/enterprise-risk.jar"]

