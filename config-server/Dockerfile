FROM maven:3.8.5-openjdk-17

WORKDIR /config-server

COPY target/config-server-0.0.1-SNAPSHOT.jar app.jar

EXPOSE 8888

CMD ["java", "-jar", "app.jar"]