FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

COPY app/build/libs/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-Xms256m", "-Xmx512m", "-jar", "-Dspring.profiles.active=prod", "app.jar"]
