# ============================================
# Dockerfile para Microservicio de USUARIOS
# Spring Boot 3.3.5 + Java 17 + PostgreSQL
# ============================================

# ============================================
# STAGE 1: Build
# ============================================
FROM maven:3.9-eclipse-temurin-17 AS build

WORKDIR /build

# Copiar solo pom.xml primero para aprovechar caché de Docker
COPY pom.xml .

# Descargar dependencias (se cachea si pom.xml no cambia)
RUN mvn dependency:go-offline -B

# Copiar el código fuente
COPY src ./src

# Compilar la aplicación (sin ejecutar tests para acelerar build)
RUN mvn clean package -DskipTests -B

# ============================================
# STAGE 2: Runtime
# ============================================
FROM eclipse-temurin:17-jre

# Metadatos de la imagen
LABEL maintainer="ondra-team"
LABEL service="usuarios-service"
LABEL version="1.0.0"

# Variables de entorno por defecto
ENV SPRING_PROFILES_ACTIVE=docker \
    SERVER_PORT=8080 \
    JAVA_OPTS="-Xms256m -Xmx512m -XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0"

# Instalar wget para health checks
RUN apt-get update && apt-get install -y wget && rm -rf /var/lib/apt/lists/*

# Crear usuario no-root para seguridad
RUN groupadd -r spring && useradd -r -g spring spring

# Cambiar a directorio de trabajo
WORKDIR /app

# Copiar el JAR compilado desde el stage de build
COPY --from=build /build/target/*.jar app.jar

# Cambiar permisos al usuario spring
RUN chown -R spring:spring /app

# Cambiar a usuario no-root
USER spring:spring

# Exponer puerto de la aplicación
EXPOSE 8080

# Health check para Azure Container Apps
HEALTHCHECK --interval=30s --timeout=5s --start-period=60s --retries=3 \
    CMD wget --no-verbose --tries=1 --spider http://localhost:8080/actuator/health || exit 1

# Comando para ejecutar la aplicación
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]