FROM openjdk:11-jre

MAINTAINER "nicktorwald"

RUN mkdir /opt/dice-roller-service \
    && cd /opt/dice-roller-service
ADD target/dice-roller-service-0.0.1-SNAPSHOT.jar app.jar

EXPOSE 8080/tcp

CMD ["java", "-Xms512m", "-Xmx512m",  "-jar", "app.jar", "--spring.profiles.active=point-dice"]
