FROM eclipse-temurin:17-jre

WORKDIR /service-registry

COPY target/service-registry-*.jar app.jar

EXPOSE 8761

CMD ["java", "-jar", "app.jar"]