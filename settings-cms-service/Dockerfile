FROM eclipse-temurin:17-jdk-alpine

# Thư mục làm việc trong container
WORKDIR /app

# Copy file jar đã build vào container
ARG JAR_FILE=target/*.jar
COPY ${JAR_FILE} app.jar

# Mở port (chỉ để document – map port vẫn do docker-compose quyết định)
EXPOSE 8080

# Lệnh chạy app
ENTRYPOINT ["java","-jar","/app.jar"]
