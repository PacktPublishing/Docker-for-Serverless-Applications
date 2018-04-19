package main

import(
	"context"
	"fmt"
	"time"

	"github.com/docker/docker/api/types"
	"github.com/docker/docker/api/types/filters"
	// "github.com/docker/docker/api/types/swarm"
	"github.com/docker/docker/client"
)

func main() {
	ctx := context.Background()

	cli, err := client.NewClient("tcp://docker-api:2375", "1.35", nil, nil)
	if err != nil {
		panic(err)
	}

	filter := filters.NewArgs(filters.Arg("type", "node"))
	msgCh, _ := cli.Events(context.Background(), types.EventsOptions{
		Filters: filter,
	})

	for {
		msg := <- msgCh
		// node created
		if msg.Action == "create" || msg.Action == "remove" {
			// list service
			serviceFilters := filters.NewArgs(filters.Arg("label", "rebalance.on.node." + msg.Action + "=true"))
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

		}

		time.Sleep(2 * time.Second)
	}
}