# ---- Build stage ----
FROM eclipse-temurin:21-jdk-alpine AS build
WORKDIR /app

# Capa de dependencias separada del código fuente: solo se re-descarga cuando cambia
# build.gradle, no en cada cambio de un .java (cache de capas de Docker).
COPY gradlew settings.gradle build.gradle ./
COPY gradle ./gradle
RUN chmod +x gradlew && ./gradlew --no-daemon dependencies > /dev/null 2>&1 || true

COPY src ./src
RUN ./gradlew --no-daemon bootJar -x test

# ---- Runtime stage ----
FROM eclipse-temurin:21-jre-alpine
RUN addgroup -S spring && adduser -S spring -G spring
WORKDIR /app
COPY --from=build /app/build/libs/*.jar app.jar
USER spring:spring

EXPOSE 8080
ENV JAVA_OPTS=""

# Azure Container Apps / App Service usan esto (o el equivalente de la plataforma) como
# liveness/readiness probe contra el endpoint de actuator ya expuesto en application.properties.
HEALTHCHECK --interval=30s --timeout=3s --start-period=45s --retries=3 \
  CMD wget -qO- http://localhost:8080/actuator/health | grep -q '"status":"UP"' || exit 1

# ZGC generacional: baja latencia de pausas con el heap chico de un plan free/burstable de
# ACA. En JDK 21 (a diferencia de JDK 23+) generational mode NO es el default de ZGC, así
# que ambas flags son obligatorias juntas para activarlo -- no alcanza con -XX:+UseZGC solo.
ENTRYPOINT ["sh", "-c", "java -XX:+UseZGC -XX:+ZGenerational $JAVA_OPTS -jar app.jar"]
