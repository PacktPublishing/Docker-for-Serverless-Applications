package main

import (
	"context"
	"fmt"
	"io/ioutil"
	"net/http"
	"time"

	"github.com/docker/docker/api/types"
	"github.com/docker/docker/api/types/filters"
	"github.com/docker/docker/api/types/swarm"
	"github.com/docker/docker/client"
)

const (
	requestNotFoundError                = "404 Not Found"
	requestFound                        = "200 OK"
	timeFormat                          = "2006-01-02T15:04:05Z07:00" // RFC 3339
	timeThresholdInterval time.Duration = 2 * time.Second
)

var (
	urlTermination          = "http://169.254.169.254/latest/meta-data/spot/termination-time"
	requestTimeoutThreshold = 150 * time.Millisecond
)

func lookupInstanceMetadata() (timestamp time.Time, err error) {
	zero, _ := time.Parse(timeFormat, "")

	// set shorter timeout as default is way too high for this operation
	req := http.Client{Timeout: requestTimeoutThreshold}
	resp, errs := req.Get(urlTermination)

	if resp != nil {
		defer resp.Body.Close()
	}

	// return error if request times out
	if errs != nil {
		err = fmt.Errorf("[!] Is this running on an EC2 Instance? Details: %v", errs)
		return
	}

	notification, errs := ioutil.ReadAll(resp.Body)
	if errs != nil {
		err = fmt.Errorf("[!] An error occurred while reading response: %v", errs)
		return
	}

	// While instance is not marked for termination EC2 should keep returning HTTP 404
	switch resp.Status {
	case requestNotFoundError:
		return zero, nil
	case requestFound:
		timestamp, _ = time.Parse(timeFormat, string(notification))
		return timestamp, nil
	default:
		return zero, nil
	}
}

const (
	managerHost = "tcp://docker-api:2375"
)

func drain(nodeID string) error {
	cli, err := client.NewClient(managerHost, "1.30", nil, nil)
	if err != nil {
		return err
	}

	node, _, err := cli.NodeInspectWithRaw(context.Background(), nodeID)
	if err != nil {
		return err
	}

	node.Spec.Availability = swarm.NodeAvailability("drain")
	err = cli.NodeUpdate(context.Background(), node.ID, node.Version, node.Spec)
	if err != nil {
		return err
	}

	return nil
}

func remove(nodeID string) error {
	cli, err := client.NewClient(managerHost, "1.30", nil, nil)
	if err != nil {
		return err
	}

	err = cli.NodeRemove(context.Background(), nodeID, types.NodeRemoveOptions{Force: true})
	if err != nil {
		return err
	}

	return nil
}

func waitForNodeDrained(nodeID string) (bool, error) {
	cli, err := client.NewClient(managerHost, "1.30", nil, nil)
	if err != nil {
		return false, err
	}

	node, _, err := cli.NodeInspectWithRaw(context.Background(), nodeID)
	if err != nil {
		return false, err
	}

	filter := filters.NewArgs()
	filter.Add("node", node.ID)

	nodeTasks, err := cli.TaskList(context.Background(), types.TaskListOptions{Filters: filter})
	if err != nil {
		return false, err
	}

	notRunningTasks := 0
	for _, t := range nodeTasks {
		if t.Status.State > swarm.TaskStateRunning {
			notRunningTasks++
		}
	}

	// done
	return notRunningTasks == len(nodeTasks), nil
}

func main() {
	const defaultDockerHost = "unix:///var/run/docker.sock"

	cli, err := client.NewClient(defaultDockerHost, "1.30", nil, nil)
	if err != nil {
		panic(err)
	}

	info, err := cli.Info(context.Background())
	if err != nil {
		fmt.Println(err)
	}

	self := info.Swarm.NodeID

	fmt.Println("Self Node ID = ", self)

	for {

		t, err := lookupInstanceMetadata()
		if err == nil {
			if !t.IsZero() {
				err = drain(self)
				if err != nil {
					fmt.Println(err.Error())
				}
				fmt.Println("Set Self to drain ...")

				//
				for {
					done, err := waitForNodeDrained(self)
					if err != nil {
						fmt.Println(err.Error())
					}
					if done {
						fmt.Println("Self is now drained ...")
						time.Sleep(1 * time.Second)
						break;
					}
					time.Sleep(1 * time.Second)
				}

				err = remove(self)
				if err != nil {
					fmt.Println(err.Error())
				}
				fmt.Println("Removed self from cluster...")
				break
			}
		} else {
			fmt.Println(err.Error())
		}

		time.Sleep(timeThresholdInterval)
	}
}
