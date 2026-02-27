# Build stage
FROM eclipse-temurin:17-jdk-alpine AS build
WORKDIR /app

COPY pom.xml .
RUN apk add --no-cache maven && mvn dependency:go-offline -B

COPY src ./src
RUN mvn package -DskipTests -B

# Run stage
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
RUN apk add --no-cache dumb-init
COPY --from=build /app/target/*.jar app.jar
EXPOSE 8081
ENTRYPOINT ["dumb-init", "--"]
CMD ["java", "-jar", "app.jar"]
