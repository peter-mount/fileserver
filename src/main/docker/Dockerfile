FROM area51/java:serverjre-8

# Default file locations
ENV CONFIGURATION_DIR   /opt/fileserver/etc
ENV FILESYSTEM_BASE     /opt/fileserver/data

ADD fileserver-1.0-SNAPSHOT-assembly.tar /opt

CMD ["/opt/fileserver/fileserver"]
