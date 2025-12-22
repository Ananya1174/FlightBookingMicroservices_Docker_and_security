FROM maven:3.8.5-openjdk-17

WORKDIR /api-gateway

COPY target/api-gateway-0.0.1-SNAPSHOT.jar app.jar

EXPOSE 8087

CMD ["java", "-jar", "app.jar"]