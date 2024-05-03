FROM amazoncorretto:17
ARG JAR_FILE=target/*.jar
COPY ${JAR_FILE} user.jar
CMD apt-get update -y
EXPOSE 8080
ENTRYPOINT ["java", "-Xmx2048M", "-jar", "/user.jar"]