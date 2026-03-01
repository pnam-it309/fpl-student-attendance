# Stage 1: Build
FROM gradle:8-jdk17 AS build
WORKDIR /app
COPY . .
RUN chmod +x gradlew
RUN ./gradlew build -x test --no-daemon

# Stage 2: Runtime
FROM eclipse-temurin:17-jre
WORKDIR /app

# Cài đặt các thư viện cần thiết cho ONNX/DJL và Graphics (AWT)
RUN apt-get update && apt-get install -y --no-install-recommends \
    libfontconfig1 \
    libstdc++6 \
    libgcc-s1 \
    libxext6 \
    libxrender1 \
    libxtst6 \
    && rm -rf /var/lib/apt/lists/*

# Copy file jar từ stage build
COPY --from=build /app/build/libs/*.jar app.jar

# Copy thư mục model
COPY model ./model

# Tạo thư mục logs
RUN mkdir /logs && chown 1000:1000 /logs

# Cấu hình bộ nhớ và cổng
# Giảm Xmx xuống 256m để dành RAM cho ONNX Native Memory (off-heap)
ENV JAVA_OPTS="-Xms128m -Xmx256m -XX:+UseSerialGC -Dfile.encoding=UTF-8 -Djava.awt.headless=true"
ENV SERVER_PORT=8765

EXPOSE 8765

# Chạy ứng dụng
# Lưu ý: Các biến MYSQL_HOST, MYSQL_USER, MYSQL_PASSWORD sẽ được lấy từ Render Dashboard Environment
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -Dserver.port=${PORT:-$SERVER_PORT} -jar app.jar"]
