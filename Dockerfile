# Dockerfile
# Multi-stage build
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
COPY docs ./docs
RUN mvn clean package -DskipTests

FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
COPY --from=build /app/docs ./docs
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]