# Imagen del backend Spring Boot (US-13).
#   docker build .                                   -> imagen de ejecución (sin pruebas)
#   docker build --target reportes --output reportes .  -> ejecuta las pruebas y exporta JUnit y cobertura (CI)

# --- Dependencias Maven (capa en caché mientras no cambie el pom) ---
FROM eclipse-temurin:25-jdk AS deps
WORKDIR /build
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN sed -i 's/\r$//' mvnw && chmod +x mvnw && ./mvnw -B -q dependency:go-offline

# --- Pruebas: los fallos no cortan el build para poder exportar los reportes; CI decide con ellos ---
FROM deps AS test
COPY src/ src/
RUN ./mvnw -B test -Dmaven.test.failure.ignore=true

FROM scratch AS reportes
COPY --from=test /build/target/surefire-reports/ /backend/
COPY --from=test /build/target/site/jacoco/ /cobertura/backend/

# --- Compilación ---
FROM deps AS build
COPY src/ src/
RUN ./mvnw -B -q package -DskipTests && cp target/*.jar app.jar

# --- Ejecución solo con JRE y usuario sin privilegios ---
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
