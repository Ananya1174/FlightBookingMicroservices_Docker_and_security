FROM maven:3.8.5-openjdk-17

WORKDIR /flight-service

COPY target/flight-service-0.0.1-SNAPSHOT.jar app.jar

EXPOSE 8081

CMD ["java", "-jar", "app.jar"]