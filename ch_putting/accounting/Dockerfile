FROM openjdk:8u151-jdk-alpine

COPY ./build/libs/accounting-0.1.war /app.war

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app.war"]
