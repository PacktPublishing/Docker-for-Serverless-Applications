# runf

runf 
rootless runc for function

runc is a project under 

user namespace support for the docker engine

what is OCI?
OpenContainer Initiative

collaboration 
container runtime configuration

runc abd libcontainer.

and image specification.

towards 1.0 specification.

what is runc.
it is a client wrapper aroud libcontainer.
Libcontainer is an interface to operating system.

docker run with parameter could be converted to config.json.

runc is potentialy open innovation platform.

implement low-level container features.
one of them is user namespace.

low bar of dependencies = 
single binary + physical rootfs bundle + json config

runc implement an environment
OCI spec compliant binary.

you already have runc when installing docker.
docker use runc via containerd, another standard.

uidmapshift

start with OCI spec file.

use alpine container.

map non-root user to root 

undocker.py to prepare rootfs for the 
```
docker save busybox | ./undocker.py -o rootfs -W -i busybox
```

the above command export all layers of busybox to stdin of undocker.

Using the docker export and docker create together also archieve the same result, but with docker daemon install.

how could we use runf?
Here the scenario.

we want an immutable version of function with readonly and rootless. Then there is a network constraint that the function should not aware of any network related configuration.

All current FaaS platforms we have implemented so far have this limitation. Say we need to attach a running function to a certain network in order to make it work correctly, be able to resolve names of other dependent services.

but with this execution model, the function proxy is responsible to attaching itself to the networks. Then function container will use the host network to communicate with other services.

So if the the function container running inside the container of the function proxy, all network configuration could be eliminated. This is what Docker calls Docker in Docker or DIND.

But with a specific container runtime like runF, this way is possible to archive of course with highest possible performance as we could cache all necessary file systems inside each function proxy. This is similar to how the mechanism of hot functions work.

Now let's see what inside runf implementation to make meets all requirements of 1) immutable, 2) rootless, 3) host networking by default and 4) zero configuration.

Hint. We mostly use the libcontainer APIs directly.

Here's all codes.

```
code
```

How could we prepare and build runf.

```
go get
go get

go build
```

now let's test running some containers.

Start with busybox.

```
./runf /bin/sh

/# ls -al
```

What's next?

With ./runf it is a potentially way to move forward another step of fast & immmutable function with a special runtime. What the reader could try is to implement a proxy container wrapping around ./runf make it run functions inside the real platform. As usual, this is left as an (advanced) exercise.