FROM amazoncorretto:17 AS runtime
ARG INSTALL_AWS_CLI=false
ARG JAR_PATH=target/zoom-data-fence-jar-with-dependencies.jar
ENV DFENCE_JAR_PATH="/app/app.jar"
COPY ${JAR_PATH} ${DFENCE_JAR_PATH}
COPY dfence /usr/bin/dfence

# Create app user for backward compatibility
RUN yum install -y shadow-utils && \
    useradd -r -s /bin/bash -d /app app && \
    mkdir -p /app && \
    chown app:app /app && \
    yum remove -y shadow-utils && \
    yum clean all

# Install AWS CLI if requested
WORKDIR /app
RUN if [ "$INSTALL_AWS_CLI" = "true" ]; then \
        yum update -y && yum install -y unzip && \
        ARCH=$(uname -m) && \
        if [ "$ARCH" = "x86_64" ]; then \
            curl "https://awscli.amazonaws.com/awscli-exe-linux-x86_64.zip" -o "awscliv2.zip"; \
        elif [ "$ARCH" = "aarch64" ]; then \
            curl "https://awscli.amazonaws.com/awscli-exe-linux-aarch64.zip" -o "awscliv2.zip"; \
        else \
            echo "Unsupported architecture: $ARCH" && exit 1; \
        fi && \
        unzip awscliv2.zip && \
        ./aws/install && \
        rm -rf awscliv2.zip aws && \
        yum remove -y unzip && yum clean all && \
        aws --version; \
    fi

USER app
ENTRYPOINT ["dfence"]