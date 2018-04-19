docker network create -d overlay parse_net

docker volume create mongo_data

docker stack deploy -c mongodb.yml         parse_01
docker stack deploy -c parse.yml           parse_02
docker stack deploy -c parse_dashboard.yml parse_03
docker stack deploy -c ingress.yml         parse_04
