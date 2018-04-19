docker run \
   --name fnserver \
   --detach \
   -v /var/run/docker.sock:/var/run/docker.sock \
   -v fn_vol:/app/data \
   -p 28080:8080 \
   --network=parse_net \
   --network-alias=fn_gateway \
   fnproject/fnserver
