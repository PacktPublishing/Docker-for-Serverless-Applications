CGO_ENABLED=0 go build -a -tags netgo \
  -ldflags '-extldflags "-static"' \
  service-balancer.go

sleep 1

docker build -t chanwit/service-balancer .
docker push chanwit/service-balancer
