FROM maven:3.9-eclipse-temurin-21 AS build

WORKDIR /workspace
COPY proto /workspace/proto
COPY backend /workspace/backend
RUN mvn -B -ntp -f backend/pom.xml clean package

FROM eclipse-temurin:21-jre

WORKDIR /app
COPY --from=build /workspace/backend/target/*.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
