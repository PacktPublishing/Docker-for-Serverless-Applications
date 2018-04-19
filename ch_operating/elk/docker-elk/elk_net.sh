docker network create \
  --driver weaveworks/net-plugin:2.1.3 \
  --subnet 10.32.200.0/24 \
  --attachable \
  elk_net
