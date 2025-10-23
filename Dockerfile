FROM eclipse-temurin:25
RUN mkdir /opt/app
COPY target/dependency-jars /opt/app/dependency-jars
COPY target/load-flow-service.jar /opt/app
CMD ["java", "-jar", "/opt/app/load-flow-service.jar"]