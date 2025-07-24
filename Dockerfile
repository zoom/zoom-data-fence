# Build the app using Maven and the cached dependencies
# (This only happens when your source code changes or you clear your docker image cache)
# Should work offline, but https://issues.apache.org/jira/browse/MDEP-82
FROM maven:3-eclipse-temurin-17 as build
COPY pom.xml /build/
COPY src /build/src
WORKDIR /build/
RUN mvn --batch-mode --fail-fast package

# Run the application (using the JRE, not the JDK)
# This assumes that your dependencies are packaged in application.jar
FROM eclipse-temurin:17-jre as runtime
COPY --from=build /build/target/application.jar ./application.jar
EXPOSE 8080
CMD ["java", "-jar", "./application.jar"]