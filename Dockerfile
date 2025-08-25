FROM maven:3.9-amazoncorretto-17-debian AS build
COPY pom.xml /build/
COPY src /build/src
WORKDIR /build/
RUN mvn --batch-mode --fail-fast -P assembly package

FROM amazoncorretto:17 AS runtime
ARG INSTALL_AWS_CLI=false
ENV DFENCE_JAR_PATH="/app/app.jar"
COPY --from=build /build/target/zoom-data-fence-jar-with-dependencies.jar ${DFENCE_JAR_PATH}
COPY dfence /usr/bin/dfence

# Install AWS CLI if requested
RUN if [ "$INSTALL_AWS_CLI" = "true" ]; then \
        curl "https://awscli.amazonaws.com/awscli-exe-linux-x86_64.zip" -o "awscliv2.zip" && \
        unzip awscliv2.zip && \
        ./aws/install && \
        rm -rf awscliv2.zip aws && \
        aws --version; \
    fi

USER 1000
ENTRYPOINT ["dfence"]