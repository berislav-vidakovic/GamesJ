FROM eclipse-temurin:21-jre

  WORKDIR /app

  COPY target/gamesj-0.0.1-SNAPSHOT.jar app.jar

  EXPOSE 8080

  ENTRYPOINT ["java", "-jar", "app.jar"]
  