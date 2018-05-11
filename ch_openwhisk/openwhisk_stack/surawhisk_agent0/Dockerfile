FROM alpine:3.6

COPY ./docker /bin/docker
COPY entrypoint.sh /
RUN  chmod +x /bin/docker && chmod +x /entrypoint.sh

ENTRYPOINT "/entrypoint.sh"
