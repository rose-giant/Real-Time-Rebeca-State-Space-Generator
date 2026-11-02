FROM eclipse-temurin:21-jre
WORKDIR /app
COPY target/classes ./classes
COPY target/lib ./lib

WORKDIR /artifact
ENTRYPOINT ["java", "-Xss512m", "-cp", "/app/classes:/app/lib/*", \
            "org.rebecalang.transparentactormodelchecker.realtimerebeca.Main"]