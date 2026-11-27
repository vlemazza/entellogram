FROM maven:3.10-eclipse-temurin-25 AS build

WORKDIR /build

COPY entellocore/pom.xml entellocore/pom.xml
COPY entellocore/src entellocore/src
RUN mvn -B -f entellocore/pom.xml install

COPY entellogram/pom.xml entellogram/pom.xml
COPY entellogram/src entellogram/src
RUN mvn -B -f entellogram/pom.xml package

FROM eclipse-temurin:25-jre

RUN apt-get update \
    && apt-get install -y --no-install-recommends ca-certificates ffmpeg pipx python3 \
    && groupadd --system entellogram \
    && useradd --system --gid entellogram --home-dir /app --shell /usr/sbin/nologin entellogram \
    && mkdir -p /app/config /app/entellogram-config /opt/pipx \
    && chown -R entellogram:entellogram /app /opt/pipx \
    && rm -rf /var/lib/apt/lists/*

ENV PIPX_HOME=/opt/pipx
ENV PIPX_BIN_DIR=/usr/local/bin

RUN pipx install gallery-dl \
    && pipx inject gallery-dl 'yt-dlp[default]' \
    && pipx install 'yt-dlp[default]'


WORKDIR /app
COPY --from=build --chown=entellogram:entellogram /build/entellogram/target/entellogram.jar /app/app.jar

USER entellogram

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
