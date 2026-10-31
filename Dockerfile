FROM maven:3.10-eclipse-temurin-25 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn -B -q dependency:go-offline
COPY src src
RUN mvn -B -q package -DskipTests

FROM eclipse-temurin:25-jre-alpine
RUN addgroup -S ticketwise && adduser -S ticketwise -G ticketwise
WORKDIR /app
COPY --from=build /app/target/ticketwise-*.jar app.jar
USER ticketwise
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
