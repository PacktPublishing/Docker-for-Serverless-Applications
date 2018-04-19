docker run -d \
  --name=logspout \
  --network=elk_net \
  --volume=/var/run/docker.sock:/var/run/docker.sock \
  gliderlabs/logspout \
  syslog+tcp+udp://logstash:5000
