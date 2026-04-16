# Stage 1: Build
FROM eclipse-temurin:17-jdk-jammy AS builder
WORKDIR /app
COPY target/auth-service-0.0.1-SNAPSHOT.jar app.jar
RUN java -Djarmode=layertools -jar app.jar extract

# Stage 2: Runtime (nhỏ hơn, an toàn hơn)
FROM eclipse-temurin:17-jre-jammy
WORKDIR /app

# Tạo non-root user
RUN groupadd -r appgroup && useradd -r -g appgroup appuser

# Copy layers từ stage 1
COPY --from=builder /app/dependencies/ ./
COPY --from=builder /app/spring-boot-loader/ ./
COPY --from=builder /app/snapshot-dependencies/ ./
COPY --from=builder /app/application/ ./

# Chạy với non-root user
USER appuser

EXPOSE 8081
ENTRYPOINT ["java", "org.springframework.boot.loader.launch.JarLauncher"]