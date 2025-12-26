FROM eclipse-temurin:17-jre

WORKDIR /config-server

COPY target/config-server-*.jar app.jar

EXPOSE 8888


CMD ["java", "-jar", "app.jar"]