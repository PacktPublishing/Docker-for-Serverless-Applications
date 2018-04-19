docker stop parity_dev
docker run --rm --name=parity_dev -d -p 8545:8545 -p 8180:8180 \
	 --network=parse_net \
	 --network-alias=blockchain \
	 parity/parity:stable-release \
	 --geth --chain dev --force-ui \
	 --reseal-min-period 0 \
	 --jsonrpc-cors http://localhost \
	 --jsonrpc-apis all \
	 --jsonrpc-interface 0.0.0.0 \
	 --jsonrpc-hosts all
sleep 2
truffle exec scripts/unlock.js
truffle migrate
