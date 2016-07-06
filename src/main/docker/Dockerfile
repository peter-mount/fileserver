FROM area51/kernel:latest
MAINTAINER Peter Mount <peter@retep.org>

# Default configuration file location
ENV CONFIGURATION_DIR               /opt/fileserver/etc

# Default cache base directory
ENV FILESYSTEM_BASE                 /opt/fileserver/data

# Default location of AWS credentials if S3 is used
ENV AWS_CREDENTIAL_PROFILES_FILE    /opt/fileserver/etc/.aws

RUN apk add --update \
        tar \
        dpkg \
        dpkg-dev \
        gnupg \
        &&\
    rm -rf /var/cache/apk/*

ADD ${project.build.finalName}-assembly.tar /opt/kernel/
