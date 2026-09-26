FROM eclipse-temurin:17-jdk-alpine AS build

WORKDIR /app

COPY .mvn .mvn
COPY mvnw pom.xml ./

RUN chmod +x mvnw
RUN ./mvnw --batch-mode dependency:go-offline

COPY src src

RUN ./mvnw --batch-mode clean package -DskipTests


FROM eclipse-temurin:17-jre-alpine

RUN addgroup -S mandateguard \
    && adduser -S mandateguard -G mandateguard

WORKDIR /app

COPY --from=build /app/target/*.jar app.jar

USER mandateguard

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]