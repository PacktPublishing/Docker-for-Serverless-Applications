# Dockerfile for example whisk docker action
FROM openwhisk/dockerskeleton:1.0.0

### Add source file(s)
ADD example.c /action/example.c

RUN apk add --no-cache --virtual .build-deps \
        bzip2-dev \
        gcc \
        libc-dev \
### Compile source file(s)
 && cd /action; gcc -static -o exec example.c \
 && apk del .build-deps

FROM openwhisk/dockerskeleton:1.0.0

ENV FLASK_PROXY_PORT 8080

COPY --from=0 /action/exec /action/exec

CMD ["/bin/bash", "-c", "cd actionProxy && python -u actionproxy.py"]
