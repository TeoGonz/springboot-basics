# syntax=docker/dockerfile:1

# =====================================================================
# Imagen del backend. Build multi-stage: el JDK + Maven solo existen en
# el stage de compilación, así la imagen final lleva únicamente un JRE y
# el jar (mucho más liviana y con menos superficie de ataque).
#
# El build context es la RAÍZ del repositorio (ver docker-compose.yml),
# aunque este Dockerfile viva en docker/.
# =====================================================================

# ---------------------- Stage 1: compilación -------------------------
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /build

# Se copia primero solo el pom.xml y se bajan las dependencias: mientras
# el pom no cambie, Docker reutiliza esta capa y los cambios en src/ no
# obligan a descargar todo el repositorio Maven otra vez.
COPY pom.xml ./
RUN mvn -B -ntp dependency:go-offline

COPY src ./src

# -DskipTests a propósito: DemoApplicationTests.contextLoads levanta el
# contexto completo de Spring y por tanto exige un Postgres accesible,
# que no existe durante el build de la imagen.
RUN mvn -B -ntp clean package -DskipTests \
 && mv target/*.jar target/app.jar

# ------------------------ Stage 2: runtime ---------------------------
FROM eclipse-temurin:17-jre AS runtime
WORKDIR /app

# Usuario sin privilegios: la app no necesita root para escuchar en 8080.
RUN groupadd --system spring \
 && useradd --system --gid spring --home /app spring

COPY --from=build /build/target/app.jar ./app.jar

USER spring
EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
