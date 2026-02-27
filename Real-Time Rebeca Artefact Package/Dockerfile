FROM eclipse-temurin:17-jre

WORKDIR /artifact

COPY ssgen-3.1.jar .
COPY examples/ examples/

ENTRYPOINT ["java", "-jar", "ssgen-3.1.jar"]