FROM amazoncorretto:17.0.17-al2023 AS runtime
ARG INSTALL_AWS_CLI=false
ARG JAR_PATH=target/zoom-data-fence-jar-with-dependencies.jar
ENV DFENCE_JAR_PATH="/app/app.jar"

# Create app user for backward compatibility
RUN dnf update -y && dnf install -y shadow-utils && \
    useradd -r -s /bin/bash -d /home/app app && \
    mkdir -p /home/app && \
    chown app:app /home/app && \
    dnf remove -y shadow-utils && \
    dnf clean all

# Create app directory and set working directory
WORKDIR /app

# Copy application files
COPY ${JAR_PATH} ${DFENCE_JAR_PATH}
COPY dfence /usr/bin/dfence
RUN if [ "$INSTALL_AWS_CLI" = "true" ]; then \
        dnf update -y && dnf install -y unzip tar gzip && \
        ARCH=$(uname -m) && \
        if [ "$ARCH" != "x86_64" ] && [ "$ARCH" != "aarch64" ]; then \
            echo "Unsupported architecture: $ARCH" && exit 1; \
        fi && \
        curl "https://awscli.amazonaws.com/awscli-exe-linux-${ARCH}.zip" -o "awscliv2.zip" && \
        unzip awscliv2.zip && \
        ./aws/install && \
        rm -rf awscliv2.zip aws && \
        dnf remove -y unzip && dnf clean all && \
        aws --version; \
    fi

USER app
ENTRYPOINT ["dfence"]