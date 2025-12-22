FROM maven:3.8.5-openjdk-17

WORKDIR /service-registry

COPY target/service-registry-0.0.1-SNAPSHOT.jar app.jar

EXPOSE 8761

CMD ["java", "-jar", "app.jar"]