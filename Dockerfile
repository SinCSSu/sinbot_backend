# syntax=docker/dockerfile:1
#
# SinBot Backend 容器化构建（Spring Boot 4 / Java 21 / Maven 多模块）
#   · builder：maven 镜像，一次 package 聚合出 sinbot_stater 的可执行 fat jar
#   · runtime：仅 JRE，非 root 用户运行，镜像内不含任何源码或敏感配置
#
# 构建上下文必须是仓库根目录：
#   docker build -t sinbot-backend:latest .
#
# 与环境相关的配置（数据库 / Redis / 机器人凭据）全部由 Nacos 下发，
# 镜像本身与环境无关，同一镜像可直接跑在任何环境。

# ------------------------------- builder -------------------------------
FROM maven:3.9-eclipse-temurin-21 AS builder

WORKDIR /build

# 先只拷 POM：改业务代码不会触发依赖重新下载。
# 多模块下 4 个子模块的 pom 必须同时就位，否则 Maven 解析不到模块间依赖。
COPY pom.xml ./
COPY sinbot_bot/pom.xml sinbot_bot/
COPY sinbot_common/pom.xml sinbot_common/
COPY sinbot_app/pom.xml sinbot_app/
COPY sinbot_stater/pom.xml sinbot_stater/

# 依赖层：-am 连带构建被依赖模块，把第三方依赖落到缓存层。
# go-offline 对某些插件依赖无法完全解析，失败不阻断，交给下面的 package 补齐。
RUN --mount=type=cache,target=/root/.m2 \
    mvn -B -q -pl sinbot_stater -am dependency:go-offline || true

COPY sinbot_bot/src sinbot_bot/src
COPY sinbot_common/src sinbot_common/src
COPY sinbot_app/src sinbot_app/src
COPY sinbot_stater/src sinbot_stater/src

# 只有 sinbot_stater 声明了 spring-boot-maven-plugin，repackage 在此产出可执行 jar。
# 同一目录下的 *.jar.original 是 repackage 前备份，必须排除。
RUN --mount=type=cache,target=/root/.m2 \
    mvn -B -pl sinbot_stater -am clean package -DskipTests \
 && find sinbot_stater/target -maxdepth 1 -name '*.jar' ! -name '*.original' \
    -exec cp {} /build/app.jar \;

# ------------------------------- runtime -------------------------------
FROM eclipse-temurin:21-jre-jammy AS runtime

# JAVA_OPTS 用容器内存比例而非固定 -Xmx：宿主机规格变化时不需要改镜像或 compose。
# UseContainerSupport 在 21 上已默认开启，这里保留是为了显式表达意图。
ENV TZ=Asia/Shanghai \
    JAVA_OPTS="-XX:MaxRAMPercentage=75.0 -XX:+ExitOnOutOfMemoryError"

# tzdata：日志时间戳与定时任务的时区依赖它，JRE 镜像不含。
# curl：供 compose healthcheck 探 /livez。
RUN set -eux; \
    apt-get update; \
    apt-get install -y --no-install-recommends tzdata curl; \
    ln -snf "/usr/share/zoneinfo/$TZ" /etc/localtime; \
    echo "$TZ" > /etc/timezone; \
    rm -rf /var/lib/apt/lists/*; \
    useradd --create-home --uid 10001 --shell /bin/bash app

WORKDIR /app
COPY --from=builder --chown=app:app /build/app.jar /app/app.jar

USER app
EXPOSE 18080

# sh -c 展开 JAVA_OPTS；exec 让 java 成为 PID 1，正确接收 docker stop 的 SIGTERM。
ENTRYPOINT ["/bin/sh", "-c", "exec java $JAVA_OPTS -jar /app/app.jar"]
