docker run \
  --name fnserver \
  --detach \
  -v /var/run/docker.sock:/var/run/docker.sock \
  -v fn_vol:/app/data \
  -p 28080:8080 \
  --network=parse_net \
  --network-alias=fn_gateway \
  -e FN_LOG_LEVEL=debug \
  -e FN_NETWORK=parse_net \
  -e BLOCKCHAIN_SERVICE=http://172.17.0.1:8545 \
  -e FAAS_GATEWAY_SERVICE=http://192.168.1.5:9000 \
  -e WHISK_GATEWAY_SERVICE=http://192.168.1.5:9000 \
  fnproject/fnserver
