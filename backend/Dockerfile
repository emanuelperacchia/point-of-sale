# ---- Stage 1: Build ----
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app

# Cache de dependencias: solo compile+runtime (includeScope=runtime descarta test/provided/system)
# -> no bajar junit/mockito/h2 (innecesarios con -Dmaven.test.skip=true)
COPY pom.xml .
RUN mvn -B dependency:go-offline -DincludeScope=runtime

# Compilación del jar (sin compilar tests: imagen de producción no los necesita)
COPY src ./src
RUN mvn -B package -Dmaven.test.skip=true

# ---- Stage 2: Runtime ----
FROM eclipse-temurin:17-jre
# curl para el healthcheck (compose depends_on: service_healthy)
RUN apt-get update \
    && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/*

WORKDIR /app
COPY --from=build /app/target/*.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
