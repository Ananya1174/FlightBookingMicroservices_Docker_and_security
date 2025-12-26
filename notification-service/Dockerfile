FROM eclipse-temurin:17-jre

WORKDIR /notification-service

COPY target/notification-service-*.jar app.jar

EXPOSE 8090


CMD ["java", "-jar", "app.jar"]