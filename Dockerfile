# One Dockerfile for every service: docker build --build-arg SERVICE=order-service .
FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /src
COPY . .
ARG SERVICE
RUN --mount=type=cache,target=/root/.m2 mvn -B -q -pl ${SERVICE} -am package -DskipTests

FROM eclipse-temurin:21-jre-alpine
ARG SERVICE
RUN addgroup -S app && adduser -S app -G app
WORKDIR /app
COPY --from=build /src/${SERVICE}/target/${SERVICE}-1.0.0.jar app.jar
USER app
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "app.jar"]
