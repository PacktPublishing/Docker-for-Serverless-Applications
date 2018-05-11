FROM anapsix/alpine-java:8_server-jre

RUN mkdir /root/data
COPY surawhisk.war /root/

EXPOSE 8080
WORKDIR /root

VOLUME /root/data

ENTRYPOINT ["/opt/jdk/bin/java"]
CMD ["-jar", "./surawhisk.war"]
