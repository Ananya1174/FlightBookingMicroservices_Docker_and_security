FROM maven:3.8.5-openjdk-17

WORKDIR /notification-service

COPY target/notification-service-0.0.1-SNAPSHOT.jar app.jar

EXPOSE 8090

CMD ["java", "-jar", "app.jar"]