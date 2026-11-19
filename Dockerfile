# Build stage: JDK 21 + Maven wrapper, produces the executable jar.
FROM eclipse-temurin:21-jdk AS build
WORKDIR /app
COPY mvnw pom.xml ./
COPY .mvn .mvn
RUN chmod +x mvnw && ./mvnw -B -q dependency:go-offline
COPY src src
RUN ./mvnw -B -q -DskipTests package

# Runtime stage: JRE only, non-root user.
FROM eclipse-temurin:21-jre
RUN useradd --system --uid 10001 app
WORKDIR /app
COPY --from=build /app/target/fruitmachine-0.0.1-SNAPSHOT.jar app.jar
USER app
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
