# Stage 1: Build
FROM gradle:8-jdk17-alpine AS build
WORKDIR /app
COPY . .
RUN ./gradlew build -x test --no-daemon

# Stage 2: Runtime
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# Cài đặt các thư viện cần thiết cho ONNX/DJL nếu cần (thường alpine cần gcompat)
RUN apk add --no-cache gcompat libstdc++

# Copy file jar từ stage build
COPY --from=build /app/build/libs/*.jar app.jar

# Copy thư mục model (quan trọng cho nhận diện khuôn mặt)
COPY model ./model

# Tạo thư mục logs
RUN mkdir /logs && chown 1000:1000 /logs

EXPOSE 8765

ENTRYPOINT ["java", "-Dfile.encoding=UTF-8", "-jar", "app.jar"]
