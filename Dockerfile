FROM maven:3.9.11-eclipse-temurin-17 AS build

WORKDIR /workspace

ARG MODULE

COPY pom.xml .
COPY rest-service/pom.xml rest-service/pom.xml
COPY email-service/pom.xml email-service/pom.xml

COPY rest-service/src rest-service/src
COPY email-service/src email-service/src

RUN mvn -pl "${MODULE}" -am package -DskipTests

FROM eclipse-temurin:17-jre

WORKDIR /app

ARG MODULE
COPY --from=build /workspace/${MODULE}/target/*.jar app.jar

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
