ARG VERSION=3.2.0
FROM maven:eclipse-temurin AS builder

WORKDIR /app

COPY pom.xml .
RUN mvn dependency:go-offline -B

COPY src ./src
RUN mvn package -DskipTests -B

FROM gcr.io/distroless/java21-debian13:latest

ARG VERSION=3.2.0

COPY --from=builder --chown=65532:65532 /app/target/smsaero-${VERSION}-cli.jar /app/smsaero-cli.jar

ENTRYPOINT ["java", "-jar", "/app/smsaero-cli.jar"]
CMD ["--help"]
