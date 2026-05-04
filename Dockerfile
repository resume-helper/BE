FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

COPY app/build/libs/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "-Dspring.profiles.active=prod", "app.jar"]
