# =============================================================================
# 后端镜像（多阶段构建）
#
# 设计要点：
#   1. 构建阶段用 Maven + JDK17，运行阶段只保留 JRE：镜像从 ~600MB 降到 ~180MB；
#   2. 先复制 pom 再复制源码，利用 Docker 层缓存（依赖不变时不重复下载）；
#   3. 迁移脚本在 ../admintem-docs/db/changelog，pom 通过相对路径引用，
#      因此构建上下文必须是**仓库根**（docker build -f admintem-backend/Dockerfile .）；
#   4. 以非 root 用户运行，减少容器逃逸后的影响面；
#   5. 时区固定 Asia/Shanghai（与 application.yml 的 serverTimezone 一致）；
#   6. 健康检查直接打 Actuator，便于编排系统判活。
#
# 构建示例：
#   docker build -f admintem-backend/Dockerfile -t admintem-backend:1.0.0 .
# 运行示例：
#   docker run -p 8090:8090 --env-file .env admintem-backend:1.0.0
#   （.env 至少包含 ADMINTEM_DB_PASSWORD / ADMINTEM_REDIS_PASSWORD）
# =============================================================================

# ---------- 构建阶段 ----------
FROM maven:3.9-eclipse-temurin-17 AS builder
WORKDIR /workspace

# 先只拷 pom，最大化依赖层缓存
COPY admintem-backend/pom.xml admintem-backend/pom.xml
RUN mvn -B -q -f admintem-backend/pom.xml dependency:go-offline

# 再拷源码与迁移脚本（脚本按 pom 里的 ../admintem-docs 相对路径引用）
COPY admintem-backend/src admintem-backend/src
COPY admintem-docs/db/changelog admintem-docs/db/changelog
RUN mvn -B -f admintem-backend/pom.xml -DskipTests package

# ---------- 运行阶段 ----------
FROM eclipse-temurin:17-jre-jammy
WORKDIR /app

# 时区与本地化：日志时间、SQL 时间统计都要与业务时区一致
ENV TZ=Asia/Shanghai \
    LANG=C.UTF-8 \
    JAVA_OPTS="-XX:MaxRAMPercentage=75 -XX:+UseG1GC -Dfile.encoding=UTF-8"

RUN apt-get update \
 && apt-get install -y --no-install-recommends curl tzdata \
 && rm -rf /var/lib/apt/lists/* \
 && groupadd -r admintem && useradd -r -g admintem -d /app -s /sbin/nologin admintem \
 && mkdir -p /app/logs /app/config \
 && chown -R admintem:admintem /app

COPY --from=builder /workspace/admintem-backend/target/admintem-backend.jar /app/app.jar

USER admintem
EXPOSE 8090

# 探活：Actuator health（已在安全白名单内，无需令牌）
HEALTHCHECK --interval=30s --timeout=5s --start-period=60s --retries=3 \
  CMD curl -fsS http://127.0.0.1:8090/actuator/health || exit 1

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/app.jar"]
