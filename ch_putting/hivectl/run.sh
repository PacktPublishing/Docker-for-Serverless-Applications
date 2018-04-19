docker run -d --network=chrome_net --network-alias=chrome --cap-add=SYS_ADMIN justinribeiro/chrome-headless

docker run --rm --network=chrome_net -v $PWD/tmp:/tmp chanwit/app-chromeless

docker run -d --network=chrome_net --network-alias=hivemind moqui/hivemind

echo '{"accountId":"55700","amount":"-5.21"}'  | docker run -i --rm --network=chrome_net -v $PWD/tmp:/tmp chanwit/app-chromeless


====

docker run -d --network=func_functions --network-alias=chrome --cap-add=SYS_ADMIN justinribeiro/chrome-headless
docker run -p 10000:80 -d --network=func_functions --network-alias=hivemind moqui/hivemind

echo '{"accountId":"55700","amount":"-5.21"}'  | docker run -i --rm --network=func_functions -v $PWD/tmp:/tmp chanwit/hivectl:0.3