# ---- Build aşaması ----
# Testler burada KOŞMAZ (-DskipTests): test + coverage gate CI'da çalışır (ADR-0003/0004).
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
# Bağımlılıkları önce indir: pom değişmedikçe Docker layer cache çalışır
COPY pom.xml .
RUN mvn -q -B dependency:go-offline
COPY src ./src
RUN mvn -q -B package -DskipTests

# ---- Çalıştırma aşaması ----
FROM eclipse-temurin:21-jre
WORKDIR /app

# Healthcheck için curl gerekli (temurin jre imajında yok)
RUN apt-get update \
    && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/* \
    && groupadd -r meldehub && useradd -r -g meldehub meldehub

USER meldehub
COPY --from=build /app/target/*.jar app.jar

EXPOSE 8080

HEALTHCHECK --interval=15s --timeout=5s --start-period=60s --retries=10 \
    CMD curl -fsS http://localhost:8080/actuator/health | grep -q '"status":"UP"' || exit 1

ENTRYPOINT ["java", "-jar", "app.jar"]
