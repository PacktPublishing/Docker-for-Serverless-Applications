CGO_ENABLED=0 go build -a -tags netgo -ldflags '-extldflags "-static"' spot-agent.go
docker build -t chanwit/spot-agent .
docker push chanwit/spot-agent
