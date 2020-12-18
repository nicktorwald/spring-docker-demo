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

## Evolution \#4

The only thing remains is to provide build reproducibility and make consistent images.

```diff
--- Dockerfile
+++ Dockerfile
@@ -1,4 +1,4 @@
-FROM openjdk:11-jre
+FROM openjdk:11-jdk
 
 LABEL maintainer="nicktorwald"
 
@@ -13,7 +13,23 @@
     && chown --recursive dice-roller:dice-roller ${APP_ROOT}
 
 WORKDIR ${APP_ROOT}
-COPY target/dice-roller-service-0.0.1-SNAPSHOT.jar app.jar
+COPY . .
+
+ARG MAVEN_VERSION=3.6.3
+ARG USER_HOME_DIR="/root"
+ARG MAVEN_URL=https://apache.osuosl.org/maven/maven-3/${MAVEN_VERSION}/binaries/apache-maven-${MAVEN_VERSION}-bin.tar.gz
+
+RUN mkdir --parents /usr/share/maven \
+    && curl --fail --silent --location --output /tmp/apache-maven.tar.gz ${MAVEN_URL} \
+    && tar --get --gzip --file=/tmp/apache-maven.tar.gz --directory=/usr/share/maven --strip-components=1 \
+    && rm --force /tmp/apache-maven.tar.gz \
+    && ln --symbolic /usr/share/maven/bin/mvn /usr/bin/mvn
+
+ENV MAVEN_HOME /usr/share/maven
+ENV MAVEN_CONFIG "$USER_HOME_DIR/.m2"
+
+RUN mvn --errors --batch-mode package \
+    && cp target/dice-roller-service-0.0.1-SNAPSHOT.jar ./app.jar
 
 EXPOSE 8080/tcp
```

Make an image but without project building at this time:  

```shell
$ docker image build -t nicktorwald/dice-roller-service:evol4 .
$ docker container run --rm -it -p 8080:8080 nicktorwald/dice-roller-service:evol4
```

### Props:

- free of any preconditions such as an already compiled service;
- reproducible and built in a consistent environment.

### Cons:

- sluggish;
- polluted by the compile-time dependencies and has a quite large size;
- has a wrong instruction order that breaks a build cache.

### Advice:

- try to create autonomous images that are independent on external environments.

## Evolution \#5

Let's split service building and deployment using Docker *multi-stage* approach: 

```diff
--- Dockerfile
+++ Dockerfile
@@ -1,4 +1,16 @@
-FROM openjdk:11-jdk
+FROM maven:3.6.3-openjdk-11 AS java-builder
+
+ENV MAVEN_OPTS -XX:+TieredCompilation -XX:TieredStopAtLevel=1
+
+WORKDIR /source
+COPY pom.xml .
+RUN mvn --threads 1C --errors --batch-mode dependency:resolve-plugins dependency:go-offline
+COPY src ./src
+RUN mvn --threads 1C --errors --batch-mode --offline package
+
+# ---
+
+FROM openjdk:11-jre
 
 LABEL maintainer="nicktorwald"
 
@@ -13,23 +25,7 @@
     && chown --recursive dice-roller:dice-roller ${APP_ROOT}
 
 WORKDIR ${APP_ROOT}
-COPY . .
-
-ARG MAVEN_VERSION=3.6.3
-ARG USER_HOME_DIR="/root"
-ARG MAVEN_URL=https://apache.osuosl.org/maven/maven-3/${MAVEN_VERSION}/binaries/apache-maven-${MAVEN_VERSION}-bin.tar.gz
-
-RUN mkdir --parents /usr/share/maven \
-    && curl --fail --silent --location --output /tmp/apache-maven.tar.gz ${MAVEN_URL} \
-    && tar --get --gzip --file=/tmp/apache-maven.tar.gz --directory=/usr/share/maven --strip-components=1 \
-    && rm --force /tmp/apache-maven.tar.gz \
-    && ln --symbolic /usr/share/maven/bin/mvn /usr/bin/mvn
-
-ENV MAVEN_HOME /usr/share/maven
-ENV MAVEN_CONFIG "$USER_HOME_DIR/.m2"
-
-RUN mvn --errors --batch-mode package \
-    && cp target/dice-roller-service-0.0.1-SNAPSHOT.jar ./app.jar
+COPY --from=java-builder /source/target/dice-roller-service-0.0.1-SNAPSHOT.jar ./app.jar
 
 EXPOSE 8080/tcp
```

Finally, to build all the stages, run:

```shell
$ docker image build -t nicktorwald/dice-roller-service:evol5 .
$ docker container run --rm -it -p 8080:8080 nicktorwald/dice-roller-service:evol5
```

### Props:

- an almost flawless image;
- segregates compile and runtime stages;
- uses advanced dependency resolution.

### Cons:

- a builder image has a huge size.

### Advice:

- divide a build process by more simple chain of stages;
- reuse project artifacts (dependencies, plugins, reports and so on) as often as possible.

## Evolution \#6

A typical java app is usually not homogeneous and consists of multiple parts. In terms of changeability it can be
split into the rarely modifiable *dependency layer* and frequently editable *application layer*. In case of Spring Boot,
it's possible to leverage layering support for the fat-jar produced by the build process. See more on
[Layering Docker Images](https://docs.spring.io/spring-boot/docs/2.4.1/reference/htmlsingle/#layering-docker-images) 

```diff
--- Dockerfile
+++ Dockerfile
@@ -7,6 +7,10 @@
 RUN mvn --threads 1C --errors --batch-mode dependency:resolve-plugins dependency:go-offline
 COPY src ./src
 RUN mvn --threads 1C --errors --batch-mode --offline package
+RUN mkdir target/_output \
+    && cd target/_output \
+    && mv ../*.jar ./app.jar \
+    && java -Djarmode=layertools -jar app.jar extract
 
 # ---
 
@@ -25,12 +29,16 @@
     && chown --recursive dice-roller:dice-roller ${APP_ROOT}
 
 WORKDIR ${APP_ROOT}
-COPY --from=java-builder /source/target/dice-roller-service-0.0.1-SNAPSHOT.jar ./app.jar
+COPY --from=java-builder /source/target/_output/dependencies/ ./
+COPY --from=java-builder /source/target/_output/spring-boot-loader/ ./
+COPY --from=java-builder /source/target/_output/snapshot-dependencies/ ./
+COPY --from=java-builder /source/target/_output/application/ ./
 
 EXPOSE 8080/tcp
 
 HEALTHCHECK CMD ["boot-ready.sh"]
 
+ENV EXECUTABLE org.springframework.boot.loader.JarLauncher
 ENV JAVA_OPTS -Xms512m -Xmx512m
 
 USER dice-roller
```

Let's build a layer-based image:

```shell
$ docker image build -t nicktorwald/dice-roller-service:evol6 .
$ docker container run --rm -it -p 8080:8080 nicktorwald/dice-roller-service:evol6
```

The created image can be explored using tool like `docker history`:

```shell
$ docker history nicktorwald/dice-roller-service:evol6 --format "table {{.ID}}\t{{.CreatedBy}}\t{{.Size}}"

IMAGE          CREATED BY                                      SIZE
d3e470b55981   /bin/sh -c #(nop)  CMD ["--spring.profiles.a…   0B
36325581ad67   /bin/sh -c #(nop)  ENTRYPOINT ["launch.sh"]     0B
92f8d1330cfd   /bin/sh -c #(nop)  USER dice-roller             0B
57867a9245b2   /bin/sh -c #(nop)  ENV JAVA_OPTS=-Xms512m -X…   0B
1c14725ad091   /bin/sh -c #(nop)  ENV EXECUTABLE=org.spring…   0B
6488a698ba13   /bin/sh -c #(nop)  HEALTHCHECK &{["CMD" "boo…   0B
eb9180154907   /bin/sh -c #(nop)  EXPOSE 8080/tcp              0B
bb95e6237958   /bin/sh -c #(nop) COPY dir:049e4d5a43f0f8482…   23.6kB
1d3347aef564   /bin/sh -c #(nop) COPY dir:dd3f9ddaa6fc1be20…   0B
b70fbfbb487a   /bin/sh -c #(nop) COPY dir:d503156ce92a53385…   241kB
627d9899d1a4   /bin/sh -c #(nop) COPY dir:9ffe512c59315d03b…   22.7MB
31bbc1467ba5   /bin/sh -c #(nop) WORKDIR /opt/dice-roller-s…   0B
5a3f0b5d7ee4   /bin/sh -c groupadd --gid 999 --system dice-…   329kB
122979c2db6b   /bin/sh -c #(nop)  ENV APP_ROOT=/opt/dice-ro…   0B
ef9d7f80fb0c   /bin/sh -c #(nop) COPY file:e8f5a36f2bdfc2db…   58B
cd1688154db0   /bin/sh -c #(nop) COPY file:3ba0f15d1549dd9c…   155B
fec2c5d461e0   /bin/sh -c #(nop)  LABEL maintainer=nicktorw…   0B
94321aa03ce0   /bin/sh -c set -eux;   arch="$(dpkg --print-…   126MB
<missing>      /bin/sh -c #(nop)  ENV JAVA_VERSION=11.0.9.1    0B
<missing>      /bin/sh -c { echo '#/bin/sh'; echo 'echo "$J…   27B
<missing>      /bin/sh -c #(nop)  ENV PATH=/usr/local/openj…   0B
<missing>      /bin/sh -c #(nop)  ENV JAVA_HOME=/usr/local/…   0B
<missing>      /bin/sh -c #(nop)  ENV LANG=C.UTF-8             0B
<missing>      /bin/sh -c set -eux;  apt-get update;  apt-g…   11.7MB
<missing>      /bin/sh -c set -ex;  if ! command -v gpg > /…   17.5MB
<missing>      /bin/sh -c set -eux;  apt-get update;  apt-g…   16.5MB
<missing>      /bin/sh -c #(nop)  CMD ["bash"]                 0B
<missing>      /bin/sh -c #(nop) ADD file:6014cd9d7466825f8…   114MB
```

`627d9899d1a4`, `b70fbfbb487a`, `1d3347aef564`, and `bb95e6237958` layers correspond to `dependencies`,
`spring-boot-loader`, `snapshot-dependencies`, and `application` directories in this order they were applied. Thus, the
most heavy-weight and least changeable layers is laid before other layers that positively impacts the Docker image
cache.  

For more detailed exploration it can be used a 3rd party tool like [wagoodman/dive](https://github.com/wagoodman/dive).

### Props:

- more fine-grained (cache tolerant) app packaging
- out of the box support (at least for Spring Boot)   

### Cons:

- may require an additional layer markup
- not all frameworks/plugins can support layering 

### Advice:

- divide an app to the several independent layers (say, libraries and app-specific artifacts)

## Evolution \#7

Tired of writing Dockerfiles? Meet [buildpacks](https://buildpacks.io/) which solves all app packaging issues for next
deployment procsss. There is also [paketo](https://paketo.io/docs/buildpacks/language-family-buildpacks/java/) that
kindly provides a set of useful so-called *buildpacks* including support for Maven and Spring Boot apps as well.

Let's run the Java paketo buildpack that includes several sub-buildpacks such as Maven and SpringBoot
buildpacks to build and run the app respectively. The maven plugin provides an integration with *paketo* via 
`spring-boot:build-image` goal.

```shell
$ mvn spring-boot:build-image -Dspring-boot.build-image.imageName=nicktorwald/dice-roller-service:evol7
$ docker container run --rm -it -p 8080:8080 nicktorwald/dice-roller-service:evol7
```
In fact, the sequence of the buildpacks used by composite the Java buildpack already takes into account the
recommendations mentioned above in the previous evolution stages. See paketo docs to know what is actually included
from buildpacks. For instance, for [SpringBoot buildpack](https://github.com/paketo-buildpacks/spring-boot) JAR layering
is applied when `Spring-Boot-Layers-Index` exists in `<APP_ROOT>/META-INF/MANIFEST.MF`.
For more detailed information about Spring Boot plugin support, take a look at page
[Packaging OCI Images](https://docs.spring.io/spring-boot/docs/2.4.1/maven-plugin/reference/htmlsingle/#build-image)

### Props:

- no-cost approach to make target images for the most standard cases 
- a high-level way to provide deployable artefacts
- a lot of provided features to compose a target runtime images

### Cons:

- may require extra painful efforts, if there are no buildpacks that support your fits

### Advice:

- try to use production-ready 3rd party tools which do the things you need  
