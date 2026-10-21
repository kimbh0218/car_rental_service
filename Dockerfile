FROM gradle:9.6.0-jdk17 AS build

WORKDIR /workspace

COPY gradle gradle
COPY gradlew gradlew
COPY gradlew.bat gradlew.bat
COPY settings.gradle build.gradle ./
RUN chmod +x gradlew

COPY src src
# Tests run separately; keeping the image build to compilation and packaging avoids
# a second Gradle test JVM exhausting small deployment instances.
ENV GRADLE_OPTS="-Dorg.gradle.jvmargs=-Xmx256m -XX:MaxMetaspaceSize=128m"
RUN ./gradlew --no-daemon --max-workers=1 clean war

FROM tomcat:10.1-jre17-temurin

RUN apt-get update \
    && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/* \
    && groupadd --system app \
    && useradd --system --uid 10001 --gid app --create-home app \
    && rm -rf /usr/local/tomcat/webapps/* \
    && mkdir -p /var/lib/car-rental/uploads \
    && chown -R app:app /usr/local/tomcat /var/lib/car-rental

COPY --from=build --chown=app:app /workspace/build/libs/*.war /usr/local/tomcat/webapps/ROOT.war

ENV CAR_IMAGE_UPLOAD_DIR=/var/lib/car-rental/uploads
EXPOSE 8080
USER app

HEALTHCHECK --interval=30s --timeout=5s --start-period=40s --retries=3 \
    CMD curl --fail --silent --show-error http://127.0.0.1:8080/health || exit 1
