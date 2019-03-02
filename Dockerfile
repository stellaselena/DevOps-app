FROM openjdk:8-jdk-alpine
VOLUME /tmp
ARG JAR_FILE
COPY ${JAR_FILE} book-1.0-SNAPSHOT.jar
ENTRYPOINT ["java","-jar","/book-1.0-SNAPSHOT.jar"]