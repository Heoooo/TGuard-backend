FROM eclipse-temurin:17-jre

# 빌드된 스프링 부트 JAR를 이미지에 복사
ARG JAR_FILE=build/libs/*.jar
COPY ${JAR_FILE} app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app.jar"]
