# Imagen del backend Spring Boot (US-13). Las pruebas se ejecutan en CI, no al construir la imagen.

# --- Etapa 1: compilación con Maven Wrapper ---
FROM eclipse-temurin:25-jdk AS build
WORKDIR /build

# Primero solo pom y wrapper para que la capa de dependencias quede en caché
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN sed -i 's/\r$//' mvnw && chmod +x mvnw && ./mvnw -B -q dependency:go-offline

COPY src/ src/
RUN ./mvnw -B -q package -DskipTests && cp target/*.jar app.jar

# --- Etapa 2: ejecución solo con JRE y usuario sin privilegios ---
FROM eclipse-temurin:25-jre
WORKDIR /app

RUN groupadd --system app && useradd --system --gid app --home /app app \
    && mkdir -p /app/data/uploads && chown -R app:app /app

COPY --from=build --chown=app:app /build/app.jar app.jar
USER app

ENV SPRING_PROFILES_ACTIVE=prod \
    JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75 -XX:+ExitOnOutOfMemoryError"

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
