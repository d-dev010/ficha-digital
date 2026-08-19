# ============================================================
# Build stage: compila o JAR com Maven
# ============================================================
FROM maven:3.9.4-eclipse-temurin-17-alpine AS build

WORKDIR /app

# Copia somente o pom.xml primeiro para cache de dependências
COPY pom.xml .
RUN mvn dependency:go-offline -B --no-transfer-progress 2>/dev/null || true

# Copia o código-fonte e compila (pula testes no build — rodam em CI)
COPY src ./src
RUN --mount=type=cache,target=/root/.m2 \
    mvn package -DskipTests -B --no-transfer-progress

# ============================================================
# Runtime stage: imagem mínima com apenas o JRE
# ============================================================
FROM eclipse-temurin:17-jre-alpine AS runtime

WORKDIR /app

# Usuário não-root para segurança
RUN addgroup -S appgroup && adduser -S appuser -G appgroup
USER appuser

# Copia apenas o JAR final
COPY --from=build /app/target/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", \
            "-XX:+UseContainerSupport", \
            "-XX:MaxRAMPercentage=75.0", \
            "-jar", "app.jar"]
