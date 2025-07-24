FROM maven:3.9-amazoncorretto-17-debian AS build
COPY pom.xml /build/
COPY src /build/src
WORKDIR /build/
RUN mvn --batch-mode --fail-fast package

FROM amazoncorretto:17 AS runtime
COPY --from=build /build/target/zoom-data-fence-jar-with-dependencies.jar /app/app.jar
COPY docker/dfence /usr/bin/dfence
ENTRYPOINT ["dfence"]