./gradlew installDist

docker build -t chanwit/hello_fn:$1 .

docker push chanwit/hello_fn:$1

fn routes delete demo /hello_fn
fn routes create /hello_fn -i chanwit/hello_fn:$1 demo

curl localhost:8080/r/demo/hello_fn
