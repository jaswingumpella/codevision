# syntax=docker/dockerfile:1.7

FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /workspace

ARG MAVEN_MODULES="-pl api -am"
ARG MAVEN_BUILD_FLAGS="-DskipTests"

RUN apt-get update \
    && apt-get install -y curl gnupg ca-certificates graphviz \
    && curl -fsSL https://deb.nodesource.com/setup_18.x | bash - \
    && apt-get install -y nodejs \
    && npm install -g npm@10 \
    && rm -rf /var/lib/apt/lists/*

# Leverage Maven layer caching by copying only the pom files first
COPY backend/pom.xml backend/pom.xml
COPY backend/api/pom.xml backend/api/pom.xml
COPY backend/oasgen/pom.xml backend/oasgen/pom.xml
RUN --mount=type=cache,target=/root/.m2 \
    mvn -f backend/pom.xml ${MAVEN_MODULES} dependency:go-offline -B

# Copy the full backend sources and build the Spring Boot jar
COPY backend backend
COPY frontend frontend
RUN --mount=type=cache,target=/root/.m2 \
    mvn -f backend/pom.xml ${MAVEN_MODULES} package ${MAVEN_BUILD_FLAGS}

FROM eclipse-temurin:21-jre-jammy AS runtime
WORKDIR /app

RUN useradd --system --home /app --shell /usr/sbin/nologin codevision && \
    mkdir -p /app/data && \
    chown -R codevision:codevision /app

RUN apt-get update \
    && apt-get install -y --no-install-recommends graphviz \
    && rm -rf /var/lib/apt/lists/*

COPY --from=build --chown=codevision:codevision /workspace/backend/api/target/codevision-backend-api-*.jar /app/app.jar

USER codevision
VOLUME ["/app/data"]
EXPOSE 8080

# Use Docker/Kubernetes env vars (e.g., SECURITY_APIKEY) to override properties from application.yml
ENV SPRING_PROFILES_ACTIVE=default \
    GRAPHVIZ_DOT=/usr/bin/dot

ENTRYPOINT ["java","-jar","/app/app.jar"]
