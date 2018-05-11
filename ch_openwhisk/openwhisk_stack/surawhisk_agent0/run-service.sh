docker service create \
  --name openwhisk_agent \
  --mount type=bind,src=/var/run/docker.sock,target=/var/run/docker.sock \
  --mount type=bind,src=/var/lib/docker/containers,target=/var/lib/docker/containers \
  --mount type=bind,src=/usr/bin/docker-runc,target=/usr/bin/docker-runc \
  --mount type=bind,src=/run/runc,target=/run/runc \
  --mount type=bind,src=/sys/fs/cgroup,target=/sys/fs/cgroup \
  surawhisk/agent
