package main

import(
	"context"
	"fmt"
	"time"

	_ "gopkg.in/Knetic/govaluate.v2"

	"github.com/hokaccha/go-prettyjson"

	"github.com/docker/docker/api/types"
	"github.com/docker/docker/api/types/events"
	"github.com/docker/docker/api/types/filters"
	"github.com/docker/docker/api/types/swarm"
	"github.com/docker/docker/client"
)

func retryTest(service swarm.Service) int {

}

func replicasIsZero(service swarm.Service) bool {
	// "Spec": {
	//   "EndpointSpec": {
	//     "Mode": "vip"
	//   },
	//   "Labels": {
	//     "auto.balance": "true",
	//     "rebalance": "true"
	//   },
	//   "Mode": {
	//     "Replicated": {
	//       "Replicas": 0
	//     }
	//   }
	// }
	if service.Spec.Mode.Replicated != nil {
		return int(*service.Spec.Mode.Replicated.Replicas) == 0
	}

	return false
}

func replicaChangedToZero(msg events.Message) bool {
	// {
	//   "Action": "update",
	//   "Actor": {
	//     "Attributes": {
	//       "name": "nginx",
	//       "replicas.new": "2",
	//       "replicas.old": "1"
	//     },
	//     "ID": "irmh2wj144udfxz7em5eravva"
	//   },
	//   "Type": "service",
	//   "scope": "swarm",
	//   "time": 1517753916,
	//   "timeNano": 1517753916259707600
	// }
	return msg.Actor.Attributes["replicas.new"] == "0"
}

func main() {
	ctx := context.Background()

	// "tcp://docker-api:2375"
	cli, err := client.NewClient(client.DefaultDockerHost, "1.30", nil, nil)
	if err != nil {
		panic(err)
	}

	// list service
	// trigger first
	// then go to start event listener

	filter := filters.NewArgs(filters.Arg("type", "service"))
	ch, _ := cli.Events(ctx, types.EventsOptions{
		Filters: filter,
	})

	// "rebalance.auto.config=true"
	// "auto.config.url=/ping"
	// ""

	for {

		msg := <- ch
		action := msg.Action

		service , _, err := cli.ServiceInspectWithRaw(ctx,
			msg.Actor.ID,
			types.ServiceInspectOptions{})
		if err != nil {
			panic(err)
		}

		json, err := prettyjson.Marshal(service)
		if err != nil {
			panic(err)
		}
		fmt.Println(string(json))

		json, err = prettyjson.Marshal(msg)
		if err != nil {
			panic(err)
		}

		configure := false
		switch action {
		case "create":
			if replicasIsZero(service) {
				configure = true
			}
		case "update":
			if replicaChangedToZero(msg) {
				configure = true
			}
		}

		if configure {
			retryTest(service)

			// _, err := cli.ServiceUpdate(ctx,
			// 	service.ID,
			// 	service.Version,
			// 	service.Spec,
			// 	types.ServiceUpdateOptions{})
		}

		/*
		if action == "create" || action == "remove" {
			// list service
			serviceFilters := filters.NewArgs(filters.Arg("label", "rebalance.on.node." + action + "=true"))
			services, err := cli.ServiceList(ctx, types.ServiceListOptions{Filters: serviceFilters})
			if err != nil {
				fmt.Println("Error listing service: ", err.Error())
			}

			fmt.Println("Found service to re-balance: ", len(services))

			for _, s := range services {
				//service, _, err := apiClient.ServiceInspectWithRaw(context.Background(), s.ID, types.ServiceInspectOptions{})
				s.Spec.TaskTemplate.ForceUpdate++

				_, err := cli.ServiceUpdate(ctx, s.ID, s.Version, s.Spec, types.ServiceUpdateOptions{})
				if err != nil {
					fmt.Println("Error updating service: ", err.Error())
				}

				fmt.Println("Re-balanced service : ", s.Spec.Name)
			}

		}*/

		time.Sleep(2 * time.Second)
	}
}