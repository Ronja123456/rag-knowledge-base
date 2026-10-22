# 1. 指定基础镜像：使用轻量级的 JDK 17 (Alpine 版体积小)
FROM eclipse-temurin:17-jre-alpine

# 2. 设置时区为上海（非常重要，否则容器里的日志时间不对）
RUN apk add --no-cache tzdata && \
    cp /usr/share/zoneinfo/Asia/Shanghai /etc/localtime && \
    echo "Asia/Shanghai" > /etc/timezone

# 3. 设置工作目录
WORKDIR /app

# 4. 把刚才编译好的 jar 包复制到容器里，并重命名为 app.jar
# 👈 注意：如果你的 jar 包名字不一样，请把前面的名字改成你 target 目录下的真实名字
COPY target/rag-knowledge-base-0.0.1-SNAPSHOT.jar app.jar

# 5. 暴露端口（你的项目是 8081）
EXPOSE 8081

# 6. 启动命令（加了一点 JVM 内存优化参数，防止在云服务器上内存溢出）
ENTRYPOINT ["java", "-Xms256m", "-Xmx512m", "-jar", "app.jar"]