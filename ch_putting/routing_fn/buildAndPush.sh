./gradlew installDist

docker build -t chanwit/routing_fn:$1 .

docker push chanwit/routing_fn:$1

fn routes delete demo /routing_fn
fn routes create /routing_fn -i chanwit/routing_fn:$1 demo

curl localhost:28080/r/demo/routing_fn


# docker run --rm -d \
# -p 8545:8545 \
#  --network=parse_net
#  --network-alias=blockchain
# parity/parity:stable-release
# --geth --chain dev --force-ui
# --reseal-min-period 0
# --jsonrpc-cors http://localhost
# --jsonrpc-apis all
# --jsonrpc-interface 0.0.0.0
# --jsonrpc-hosts all