# ---------- Build stage ----------
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app

# Cache dependencies first (re-runs only when pom.xml changes)
COPY pom.xml .
RUN mvn -B -Pexecutable dependency:go-offline

# Build the fat jar
COPY src ./src
RUN mvn -B -Pexecutable clean package -DskipTests

# ---------- Runtime stage ----------
FROM eclipse-temurin:21-jre
WORKDIR /app

COPY --from=build /app/target/modelchecker-*.jar app.jar

ENTRYPOINT ["java", "-jar", "app.jar"]