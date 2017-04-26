FROM openjdk:8-jdk-alpine


ADD target/server-checks.jar /
ENTRYPOINT ["java", "-jar", "server-checks.jar"]
