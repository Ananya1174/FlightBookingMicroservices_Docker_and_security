FROM eclipse-temurin:17-jre
WORKDIR /api-gateway
COPY target/api-gateway-*.jar app.jar
EXPOSE 8087
CMD ["java", "-jar", "app.jar"]