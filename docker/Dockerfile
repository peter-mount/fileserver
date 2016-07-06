FROM area51/kernel
MAINTAINER Peter Mount <peter@retep.org>

ENV FILESERVER_VERSION 1.0-SNAPSHOT
ENV MAVEN_VERSION  3.3.9

RUN apk add --update \
        tar \
        dpkg \
        dpkg-dev \
        gnupg \
        &&\
    rm -rf /var/cache/apk/*

RUN mkdir -p /opt/kernel &&\
    cd /tmp &&\
    curl -sSL \
	-o maven.zip \
	https://repo.maven.apache.org/maven2/org/apache/maven/apache-maven/${MAVEN_VERSION}/apache-maven-${MAVEN_VERSION}-bin.zip &&\
    unzip maven.zip &&\
    /tmp/*maven*/bin/mvn \
	-Dmaven.repo.local=/tmp/repo \
	org.apache.maven.plugins:maven-dependency-plugin:2.10:get \
	-DrepoUrl=https://nexus.area51.onl/repository/maven-public \
	-Dartifact=onl.area51.apps:fileserver:${FILESERVER_VERSION}:tar:assembly \
	-Dtransitive=false &&\
    cd /opt/kernel &&\
    tar xf /tmp/repo/onl/area51/apps/fileserver/${FILESERVER_VERSION}/fileserver-${FILESERVER_VERSION}-assembly.tar &&\
    rm -rf /tmp/*

CMD ["/opt/kernel/start"]

