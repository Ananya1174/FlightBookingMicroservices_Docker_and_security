FROM eclipse-temurin:17-jre

WORKDIR /flight-service

COPY target/flight-service-*.jar app.jar

EXPOSE 8081

CMD ["java", "-jar", "app.jar"]