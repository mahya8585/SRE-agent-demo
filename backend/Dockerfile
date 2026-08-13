FROM maven:3.9-eclipse-temurin-8 AS build

WORKDIR /app
COPY pom.xml ./
RUN mvn dependency:go-offline -B
COPY src ./src
RUN mvn package -DskipTests -B

FROM eclipse-temurin:8-jre-jammy

WORKDIR /app
ARG APPLICATIONINSIGHTS_AGENT_VERSION=3.7.8
ARG APPLICATIONINSIGHTS_AGENT_SHA256=e8c494f3f9a36f6b99790a4741b03f51988dc5c4244c14cc24be51874ef72e6b
ADD https://github.com/microsoft/ApplicationInsights-Java/releases/download/${APPLICATIONINSIGHTS_AGENT_VERSION}/applicationinsights-agent-${APPLICATIONINSIGHTS_AGENT_VERSION}.jar applicationinsights-agent.jar
RUN echo "${APPLICATIONINSIGHTS_AGENT_SHA256}  applicationinsights-agent.jar" | sha256sum -c -
COPY --from=build /app/target/wine-demo-0.0.1-SNAPSHOT.jar app.jar
COPY applicationinsights.json applicationinsights.json

EXPOSE 8081
ENTRYPOINT ["java", "-javaagent:/app/applicationinsights-agent.jar", "-jar", "app.jar"]