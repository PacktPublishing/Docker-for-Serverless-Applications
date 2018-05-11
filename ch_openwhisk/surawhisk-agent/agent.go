package main

import (
	"context"
	"fmt"
	"time"
	"os"
	"strings"

	"github.com/docker/docker/api/types"
	"github.com/docker/docker/api/types/container"
	"github.com/docker/docker/client"
	"github.com/docker/go-connections/nat"
)

func checkInvoker(invokerIndex string) bool {

	cli, err := client.NewEnvClient()
	if err != nil {
		panic(err)
	}

	containers, err := cli.ContainerList(context.Background(), types.ContainerListOptions{})
	if err != nil {
		panic(err)
	}

	for _, container := range containers {
		/*
		if container.Image == "openwhisk/invoker" && container.Names[0] == expectedName {
			fmt.Printf("%s %s\n", container.ID[0:10], container.Names[0])
			found = true
		}*/
		fmt.Println(container.Command)
		if strings.Contains(container.Command, "invoker/bin/invoker " + invokerIndex) {
			return true
		}
	}

	return false
}

func startInvoker(name string, port string, networkName string) {
	fmt.Printf("starting %s\n", name)
	cli, err := client.NewEnvClient()
	if err != nil {
		panic(err)
	}

	_, err = cli.ContainerCreate(context.Background(),
		&container.Config{
			// copy all environment from the parent process,
			// so these can be passed thru the agent -e
			Env: os.Environ(),
			Image: "openwhisk/invoker",
		},
		&container.HostConfig{
			Binds: []string {
                "/usr/bin/docker-runc:/usr/bin/docker-runc:rw",
                "/sys/fs/cgroup:/sys/fs/cgroup:rw",
                "/var/run/docker.sock:/var/run/docker.sock:rw",
                "/var/lib/docker/containers:/containers:rw",
                "/run/runc:/run/runc:rw",
			},
			NetworkMode: networkName,
			PortBindings: nat.PortMap{
				port: []nat.PortBinding{
					{
						HostIP: "0.0.0.0",
						HostPort: "8085",
					},
				},
			},
			Privileged: true,
		},
		nil,
		name)

	// and wait until it's proper started
}

func main() {
	invokerIndex := os.Args[1]
	exposedPort := os.Args[2]
	networkName := os.Args[3]
	for {
		found := checkInvoker(invokerIndex)
		if !found {
			startInvoker(expectedName, exposedPort)
		}

		time.Sleep(1000 * time.Millisecond)
	}
}