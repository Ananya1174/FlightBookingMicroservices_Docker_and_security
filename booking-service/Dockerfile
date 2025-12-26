FROM eclipse-temurin:17-jre

WORKDIR /booking-service

COPY target/booking-service-*.jar app.jar

EXPOSE 8082


CMD ["java", "-jar", "app.jar"]