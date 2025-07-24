FROM maven:3.9-amazoncorretto-17-debian as build
COPY pom.xml /build/
COPY src /build/src
WORKDIR /build/
RUN mvn --batch-mode --fail-fast package

FROM amazoncorretto:17 as runtime
COPY --from=build /build/target/zoom-data-fence-jar-with-dependencies.jar ./application.jar
EXPOSE 8080
CMD ["java", "-jar", "./application.jar"]