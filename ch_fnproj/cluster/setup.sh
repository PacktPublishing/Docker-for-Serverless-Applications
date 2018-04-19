$ docker network create -d overlay --attachable fn_net

$ docker volume create mysql_vol

$ docker run \
  --detach \
  --network fn_net \
  --network-alias mysql \
  -e MYSQL_DATABASE=fn_db \
  -e MYSQL_USER=func \
  -e MYSQL_PASSWORD=funcpass \
  -e MYSQL_RANDOM_ROOT_PASSWORD=yes \
  -v mysql_vol:/var/lib/mysql \
  mysql

docker run --privileged \
  --detach \
  --network fn_net \
  --network-alias fn_0 \
  --name fn_0 \
  -e "FN_DB_URL=mysql://func:funcpass@tcp(mysql:3306)/fn_db" \
  fnproject/fnserver

docker run --privileged \
  --detach \
  --network fn_net \
  --network-alias fn_1 \
  --name fn_1 \
  -e "FN_DB_URL=mysql://func:funcpass@tcp(mysql:3306)/fn_db" \
  fnproject/fnserver

docker run --detach \
   --network fn_net \
   --network-alias fnlb \
   --name fnlb \
   -p 8080:8081 \
   fnproject/fnlb:latest --nodes fn_0:8080,fn_1:8080

docker run --detach \
  --network fn_net \
  --network-alias fnui \
  -p 4000:4000 \
  -e "FN_API_URL=http://fnlb:8081" fnproject/ui

docker run --detach \
  --name myadmin \
  --network fn_net \
  --network-alias myadmin \
  -p 9000:80 \
  -e PMA_HOST=mysql \
  phpmyadmin/phpmyadmin

curl -X POST -d '{"Name":"chanwit"}' http://localhost:8080/r/demo/hello_go

curl -X POST -d '' http://localhost:8080/r/demo/hello_go
