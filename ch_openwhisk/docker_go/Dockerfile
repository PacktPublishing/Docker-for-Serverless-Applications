# Compile the Go program
FROM golang:1.9.2-alpine3.6

WORKDIR /go/src/app
COPY main.go .

RUN CGO_ENABLED=0 go build -a -ldflags '-extldflags "-static"' main.go

# Build using the base image for whisk docker action
FROM openwhisk/dockerskeleton

ENV FLASK_PROXY_PORT 8080

COPY --from=0 /go/src/app/main /action/exec

CMD ["/bin/bash", "-c", "cd actionProxy && python -u actionproxy.py"]
