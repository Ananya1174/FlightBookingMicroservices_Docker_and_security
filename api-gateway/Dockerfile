# ---- build stage ----
FROM maven:3.9.6-eclipse-temurin-17 AS builder
WORKDIR /workspace

COPY pom.xml .
COPY .mvn .mvn
COPY mvnw mvnw
RUN mvn -q -N io.takari:maven:wrapper || true

COPY src ./src

RUN mvn -B -DskipTests package

# ---- runtime stage ----
FROM eclipse-temurin:17-jre
WORKDIR /app

COPY --from=builder /workspace/target/*.jar app.jar

EXPOSE 8087

ENTRYPOINT ["java", "-jar", "/app/app.jar"]