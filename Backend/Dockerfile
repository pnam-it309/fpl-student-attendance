# Stage 1: Build
FROM gradle:8-jdk17-alpine AS build
WORKDIR /app
COPY . .
RUN ./gradlew build -x test --no-daemon

# Stage 2: Runtime
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# Cài đặt các thư viện cần thiết cho ONNX/DJL
RUN apk add --no-cache gcompat libstdc++ libgcc

# Copy file jar từ stage build
COPY --from=build /app/build/libs/*.jar app.jar

# Copy thư mục model
COPY model ./model

# Tạo thư mục logs
RUN mkdir /logs && chown 1000:1000 /logs

# Biến môi trường mặc định cho Java Heap
# -Xmx512m: Giới hạn tối đa 512MB RAM (có thể tăng nếu host của bạn có nhiều RAM hơn)
ENV JAVA_OPTS="-Xms256m -Xmx512m -Dfile.encoding=UTF-8"
ENV SERVER_PORT=8765

EXPOSE 8765

# Chạy với JAVA_OPTS và ưu tiên cổng từ biến PORT của Cloud nếu có
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -Dserver.port=${PORT:-$SERVER_PORT} -jar app.jar"]
