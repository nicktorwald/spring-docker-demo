FROM openjdk:11-jre

LABEL maintainer="nicktorwald"

WORKDIR /opt/dice-roller-service
COPY target/dice-roller-service-0.0.1-SNAPSHOT.jar app.jar

EXPOSE 8080/tcp

ENTRYPOINT ["java", "-Xms512m", "-Xmx512m",  "-jar", "app.jar"]
CMD ["--spring.profiles.active=point-dice"]
