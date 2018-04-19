gradle installDist

docker build -t chanwit/listener:$1 .
docker push chanwit/listener:$1
