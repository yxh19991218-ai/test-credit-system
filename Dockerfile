# ============================================================
# Credit System — 后端多阶段构建 Dockerfile
# ============================================================

# ---- Stage 1: Build ----
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app

# 先复制 pom.xml 并下载依赖（利用 Docker 缓存）
COPY pom.xml ./
RUN mvn dependency:go-offline -B -q || true

# 复制源码并编译
COPY src ./src
RUN mvn package -DskipTests -B -q

# ---- Stage 2: Runtime ----
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# 时区
RUN apk add --no-cache tzdata && \
    cp /usr/share/zoneinfo/Asia/Shanghai /etc/localtime && \
    echo "Asia/Shanghai" > /etc/timezone

# 从构建阶段复制 JAR
COPY --from=build /app/target/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
