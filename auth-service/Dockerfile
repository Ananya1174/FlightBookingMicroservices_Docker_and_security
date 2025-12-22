FROM maven:3.8.5-openjdk-17

WORKDIR /auth-service

COPY target/auth-service-0.0.1-SNAPSHOT.jar app.jar

EXPOSE 8085

CMD ["java", "-jar", "app.jar"]