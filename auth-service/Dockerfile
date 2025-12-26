FROM eclipse-temurin:17-jre

WORKDIR /auth-service

COPY target/auth-service-*.jar app.jar

EXPOSE 8085


CMD ["java", "-jar", "app.jar"]