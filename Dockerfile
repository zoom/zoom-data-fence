FROM maven:3.9-amazoncorretto-21-debian AS build
COPY pom.xml /build/
COPY src /build/src
WORKDIR /build/
RUN mvn --batch-mode --fail-fast -P assembly package

FROM amazoncorretto:17 AS runtime
ENV DFENCE_JAR_PATH="/app/app.jar"
COPY --from=build /build/target/zoom-data-fence-jar-with-dependencies.jar ${DFENCE_JAR_PATH}
COPY dfence /usr/bin/dfence
USER 1000
ENTRYPOINT ["dfence"]