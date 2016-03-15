FROM area51/java:serverjre-8

# Default configuration file location
ENV CONFIGURATION_DIR               /opt/fileserver/etc

# Default cache base directory
ENV FILESYSTEM_BASE                 /opt/fileserver/data

# Default location of AWS credentials if S3 is used
ENV AWS_CREDENTIAL_PROFILES_FILE    /opt/fileserver/etc/.aws

ADD fileserver-1.0-SNAPSHOT-assembly.tar /opt

CMD ["/opt/fileserver/fileserver"]
