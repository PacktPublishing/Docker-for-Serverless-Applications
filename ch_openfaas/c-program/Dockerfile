FROM alpine:3.6

RUN apk update && apk add gcc musl-dev

COPY main.c /root/
WORKDIR /root/

RUN gcc -static -o main main.c


FROM alpine:3.6
ADD https://github.com/openfaas/faas/releases/download/0.6.5/fwatchdog /usr/bin/

RUN chmod +x /usr/bin/fwatchdog
EXPOSE 8080

COPY --from=0 /root/main /usr/bin/func_c
ENV fprocess="/usr/bin/func_c"

ENTRYPOINT ["/usr/bin/fwatchdog"]
