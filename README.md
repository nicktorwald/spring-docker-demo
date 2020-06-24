# A gentle guide how to build Java apps with Docker

This is an additional repo that is used for demonstration purposes at SibEdge.

## How to use this guide   

This tutorial evolves within a number of commits where each commit improves the solution in terms of performance,
maintainability, and security.

To try it out, check out a particular commit from the `master` branch. It will roll back you to the needed evolution
state.

## Evolution \#0

Let's write some Dockerfile that just places already compiled service:

```diff
--- Dockerfile
+++ Dockerfile
@@ -0,0 +1,11 @@
+FROM openjdk:11-jre
+
+MAINTAINER "nicktorwald"
+
+RUN mkdir /opt/dice-roller-service \
+    && cd /opt/dice-roller-service
+ADD target/dice-roller-service-0.0.1-SNAPSHOT.jar app.jar
+
+EXPOSE 8080/tcp
+
+CMD ["java", "-Xms512m", "-Xmx512m",  "-jar", "app.jar", "--spring.profiles.active=point-dice"]
```

Finally, to build an image and run a container, call these commands:

```shell
$ ./mvnw clean install
$ docker image build -t nicktorwald/dice-roller-service:evol0 .
$ docker container run --rm -it -p 8080:8080 nicktorwald/dice-roller-service:evol0
```

### Props:

- it works;
- as simple as it can be;
- a fast image build (excluding a separate service build).

### Cons:

- not flexible enough;
- possibly not secure enough;
- possibly bad smells;
- requires a pre-compiled service (a JAR file to be copied).

### Advice:

- try to keep it simple and efficiently
