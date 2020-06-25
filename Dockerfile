FROM openjdk:11-jdk

LABEL maintainer="nicktorwald"

COPY distribution/boot-ready.sh /usr/local/bin/
COPY distribution/launch.sh /usr/local/bin/

ENV APP_ROOT /opt/dice-roller-service

RUN groupadd --gid 999 --system dice-roller \
    && useradd --uid 999 --system --gid dice-roller dice-roller \
    && mkdir --parents ${APP_ROOT} \
    && chown --recursive dice-roller:dice-roller ${APP_ROOT}

WORKDIR ${APP_ROOT}
COPY . .

ARG MAVEN_VERSION=3.6.3
ARG USER_HOME_DIR="/root"
ARG MAVEN_URL=https://apache.osuosl.org/maven/maven-3/${MAVEN_VERSION}/binaries/apache-maven-${MAVEN_VERSION}-bin.tar.gz

RUN mkdir --parents /usr/share/maven \
    && curl --fail --silent --location --output /tmp/apache-maven.tar.gz ${MAVEN_URL} \
    && tar --get --gzip --file=/tmp/apache-maven.tar.gz --directory=/usr/share/maven --strip-components=1 \
    && rm --force /tmp/apache-maven.tar.gz \
    && ln --symbolic /usr/share/maven/bin/mvn /usr/bin/mvn

ENV MAVEN_HOME /usr/share/maven
ENV MAVEN_CONFIG "$USER_HOME_DIR/.m2"

RUN mvn --errors --batch-mode package \
    && cp target/dice-roller-service-0.0.1-SNAPSHOT.jar ./app.jar

EXPOSE 8080/tcp

HEALTHCHECK CMD ["boot-ready.sh"]

ENV JAVA_OPTS -Xms512m -Xmx512m

USER dice-roller
ENTRYPOINT ["launch.sh"]
CMD ["--spring.profiles.active=point-dice"]
