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

## Evolution \#1

Let's analyze the Dockerfile using some linters or manually following the best practices from 
[the official docs](https://docs.docker.com/develop/develop-images/dockerfile_best-practices).

For instance, run `hadolint` to check if this Dockerfile has any known drawbacks:  

```shell
$ docker run --rm -i hadolint/hadolint < Dockerfile
```

There will be a few rules triggered such as avoid using the deprecated `MAINTAINER` instruction or using `COPY`
instead of `ADD` and so on. Let's fix them all and improve some other issues here: 

```diff
--- Dockerfile
+++ Dockerfile
@@ -1,11 +1,11 @@
 FROM openjdk:11-jre
 
-MAINTAINER "nicktorwald"
+LABEL maintainer="nicktorwald"
 
-RUN mkdir /opt/dice-roller-service \
-    && cd /opt/dice-roller-service
-ADD target/dice-roller-service-0.0.1-SNAPSHOT.jar app.jar
+WORKDIR /opt/dice-roller-service
+COPY target/dice-roller-service-0.0.1-SNAPSHOT.jar app.jar
 
 EXPOSE 8080/tcp
 
-CMD ["java", "-Xms512m", "-Xmx512m",  "-jar", "app.jar", "--spring.profiles.active=point-dice"]
+ENTRYPOINT ["java", "-Xms512m", "-Xmx512m",  "-jar", "app.jar"]
+CMD ["--spring.profiles.active=point-dice"]
```

Again, to build a new version and run a container, call these commands:

```shell
$ ./mvnw clean install
$ docker image build -t nicktorwald/dice-roller-service:evol1 .
$ docker container run --rm -it -p 8080:8080 nicktorwald/dice-roller-service:evol1
```

### Props:

- no more evident linter issues;
- a bit more flexible.

### Cons:

- still, possibly, is not secure enough;
- still is not reproducible. 

### Advice:

- learn and follow the best practices from the community;
- use linters to check Dockerfile-s (i.e. `hadolint`).

## Evolution \#2

Next, it is time to become more secure and apply another good suggestion that is concluded in running under the least
privileged user.

```diff
--- Dockerfile
+++ Dockerfile
@@ -2,10 +2,18 @@
 
 LABEL maintainer="nicktorwald"
 
-WORKDIR /opt/dice-roller-service
+ENV APP_ROOT /opt/dice-roller-service
+
+RUN groupadd --gid 999 --system dice-roller \
+    && useradd --uid 999 --system --gid dice-roller dice-roller \
+    && mkdir --parents ${APP_ROOT} \
+    && chown --recursive dice-roller:dice-roller ${APP_ROOT}
+
+WORKDIR ${APP_ROOT}
 COPY target/dice-roller-service-0.0.1-SNAPSHOT.jar app.jar
 
 EXPOSE 8080/tcp
 
+USER dice-roller
 ENTRYPOINT ["java", "-Xms512m", "-Xmx512m",  "-jar", "app.jar"]
 CMD ["--spring.profiles.active=point-dice"]
``` 

Rebuild the service:

```shell
$ ./mvnw clean install
$ docker image build -t nicktorwald/dice-roller-service:evol2 .
$ docker container run --rm -it -p 8080:8080 nicktorwald/dice-roller-service:evol2
```

Don't forget to use external smart analyzers (in the same way you use the linters) such as `snyk` to discover whether
your image has well-known vulnerabilities or not.

```shell
snyk test --docker nicktorwald/dice-roller-service:evol2 --file=Dockerfile

<skip long list of issues>

Organization:      nicktorwald
Package manager:   deb
Target file:       Dockerfile
Project name:      docker-image|nicktorwald/dice-roller-service
Docker image:      nicktorwald/dice-roller-service:evol2
Base image:        openjdk:11-jre
Licenses:          enabled

Tested 145 dependencies for known issues, found 92 issues.

Base Image      Vulnerabilities  Severity
openjdk:11-jre  92               6 high, 13 medium, 73 low

Recommendations for base image upgrade:

Alternative image types
Base Image                     Vulnerabilities  Severity
openjdk:15-ea-16               0                0 high, 0 medium, 0 low
openjdk:15-ea-18-oracle        0                0 high, 0 medium, 0 low
openjdk:15-ea-14-oraclelinux7  0                0 high, 0 medium, 0 low
openjdk:15-ea-15               0                0 high, 0 medium, 0 low
```

Thus, `snyk` says the target image has no security issues there, but the base image `openjdk:11-jre` has a lot. Maybe, it is
time to bump up the java version to 15?

### Props:

- no more evident security issues.

### Cons:

- still depends on external service build. 

### Advice:

- learn more about potential vulnerabilities and try to mitigate them;
- use analyzers to check your images as well as the images you base on (i.e. `snyk`).

## Evolution \#3

To be a more production-ready it is worth adding extra tools and health check mechanism:

```diff
--- Dockerfile
+++ Dockerfile
@@ -2,6 +2,9 @@
 
 LABEL maintainer="nicktorwald"
 
+COPY distribution/boot-ready.sh /usr/local/bin/
+COPY distribution/launch.sh /usr/local/bin/
+
 ENV APP_ROOT /opt/dice-roller-service
 
 RUN groupadd --gid 999 --system dice-roller \
@@ -14,6 +17,10 @@
 
 EXPOSE 8080/tcp
 
+HEALTHCHECK CMD ["boot-ready.sh"]
+
+ENV JAVA_OPTS -Xms512m -Xmx512m
+
 USER dice-roller
-ENTRYPOINT ["java", "-Xms512m", "-Xmx512m",  "-jar", "app.jar"]
+ENTRYPOINT ["launch.sh"]
 CMD ["--spring.profiles.active=point-dice"]
```

Rebuild the service and mount a custom configuration via the volume system:

```shell
$ ./mvnw clean install
$ docker image build -t nicktorwald/dice-roller-service:evol3 .
$ echo "spring.main.banner-mode: off" > application.yaml
$ docker container run --rm -it -p 8080:8080 \ 
    -v $(pwd)/application.yaml:/opt/dice-roller-service/config/application.yaml \
    nicktorwald/dice-roller-service:evol3
```

Let's verify that the created container is *healty* and has a config *volume* mounted:

```shell
docker container ps --filter "label=maintainer=nicktorwald" --format "table {{.ID}}\t{{.Status}}\t{{.Mounts}}"

CONTAINER ID        STATUS                   MOUNTS
9447bfb41ee3        Up 3 minutes (healthy)   /Users/nicktor…,d3f54bf9c5efd8…
```

### Props:

- contains a convenient utility belt;
- more flexible service configuration via a custom entry point.

### Cons:

- still requires a pre-compiled service (a JAR file to be copied). 

### Advice:

- consider extracting the utility belt and common preparations to the platform base image that can be used to build
  service images;
- provide the built-in heath check;
- provide at least one way for consumers to configure a container (environment variables, app properties, external
  mounted configuration files etc.). 
