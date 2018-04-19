package main

import (
	"./ssh"
	"encoding/base64"
	"encoding/json"
	"fmt"
	"os"
	"strings"
	"time"

	"github.com/aws/aws-sdk-go/aws"
	"github.com/aws/aws-sdk-go/aws/session"
	"github.com/aws/aws-sdk-go/service/ec2"
)

func getManagersByClusterId(svc *ec2.EC2, clusterId string) []*ec2.Instance {
	instances := []*ec2.Instance{}

	result, err := svc.DescribeInstances(&ec2.DescribeInstancesInput{
		Filters: []*ec2.Filter{
			{
				Name:   aws.String("tag:ClusterID"),
				Values: aws.StringSlice([]string{clusterId}),
			},
			{
				Name:   aws.String("tag:Type"),
				Values: aws.StringSlice([]string{"manager"}),
			},
		},
	})
	if err != nil {
		return nil
	}

	for _, reservation := range result.Reservations {
		for _, instance := range reservation.Instances {
			if *instance.State.Name == "running" {
				instances = append(instances, instance)
			}
		}
	}

	return instances
}

func createManagers(svc *ec2.EC2, clusterId string, size int64) []*ec2.Instance {
	runResult, err := svc.RunInstances(&ec2.RunInstancesInput{
		ImageId:      aws.String("ami-325d2e4e"),
		InstanceType: aws.String("t2.micro"),
		MinCount:     aws.Int64(size),
		MaxCount:     aws.Int64(size),
		KeyName:      aws.String("chanwit-3558"),
		BlockDeviceMappings: []*ec2.BlockDeviceMapping{
			{
				DeviceName: aws.String("/dev/sda1"),
				Ebs: &ec2.EbsBlockDevice{
					DeleteOnTermination: aws.Bool(true),
					VolumeSize:          aws.Int64(8),
				},
			},
		},
		TagSpecifications: []*ec2.TagSpecification{
			{
				ResourceType: aws.String("instance"),
				Tags: []*ec2.Tag{
					{
						Key:   aws.String("ClusterID"),
						Value: aws.String(clusterId), // TODO
					},
					{
						Key:   aws.String("Type"),
						Value: aws.String("manager"), // TODO
					},
				},
			},
		},
	})

	if err != nil {
		return nil
	}

	return runResult.Instances
}

type Node struct {
	Address string            `json:"address,omitempty"`
	User    string            `json:"user,omitempty"`
	Role    []string          `json:"role,omitempty"`
	Labels  map[string]string `json:"labels,omitempty"`
}

type JoinInfo struct {
	managerToken   string
	workerToken    string
	remoteManagers []RemoteManager
}

type RemoteManager struct {
	NodeID string `json:"NodeID,omitempty"`
	Addr   string `json:"Addr,omitempty"`
}

func getJoinInfo(user, ip string) (JoinInfo, error) {
	shell, err := ssh.NewClient(user, ip, 22, &ssh.Auth{
		Keys: Keys,
	})
	if err != nil {
		fmt.Println(err)
		return JoinInfo{}, err
	}

	remoteManagers := []RemoteManager{}
	out, err := shell.Output(`sudo docker info --format="{{json .Swarm.RemoteManagers}}"`)
	// fmt.Println(out)
	if err != nil {
		return JoinInfo{}, err
	}

	err = json.Unmarshal([]byte(out), &remoteManagers)
	// fmt.Println(err)
	if err != nil {
		return JoinInfo{}, err
	}

	joinInfo := JoinInfo{}

	out, err = shell.Output("sudo docker swarm join-token -q worker && sudo docker swarm join-token -q manager")
	// fmt.Println(out)
	if err != nil {
		return JoinInfo{}, err
	}

	tokens := strings.Split(strings.TrimSpace(out), "\n")
	joinInfo.workerToken = tokens[0]
	joinInfo.managerToken = tokens[1]
	joinInfo.remoteManagers = remoteManagers

	return joinInfo, nil
}

func joinAsMaster(user, ip string, joinInfo JoinInfo) error {
	shell, err := ssh.NewClient(user, ip, 22, &ssh.Auth{
		Keys: Keys,
	})
	if err != nil {
		fmt.Println(err)
		return err
	}

	token := joinInfo.managerToken
	addr := joinInfo.remoteManagers[0].Addr

	cmd := fmt.Sprintf("sudo docker swarm join --availability=drain --token=%s %s", token, addr)
	err = shell.Shell(cmd)
	if err != nil {
		fmt.Println(err)
		return err
	}

	return err
}

var Keys []string

func main() {
	CLUSTER_ID := "test"

	key := os.Getenv("SSH_PRIVATE_KEY")
	if key == "" {
		key = os.ExpandEnv("$HOME/.ssh/id_rsa")
	}
	Keys = []string{key}

	// Load session from shared config
	sess := session.Must(session.NewSessionWithOptions(session.Options{
		SharedConfigState: session.SharedConfigEnable,
	}))

	// Create new EC2 client
	svc := ec2.New(sess)

	mgInstances := getManagersByClusterId(svc, CLUSTER_ID)
	mg_count_1st_check := len(mgInstances)

	if mg_count_1st_check > 0 {
		for _, i := range mgInstances {
			fmt.Println("Manager Inst:", *i.InstanceId)
		}
	}

	if mg_count_1st_check != 3 {
		instances := createManagers(svc, CLUSTER_ID, int64(3-mg_count_1st_check))
		for _, i := range instances {
			fmt.Println("Created Manager Inst:", *i.InstanceId)
		}
	}

	var joinInfo JoinInfo

	// newly created cluster
	if mg_count_1st_check == 0 {

		// obtain mgInstances again
		for {
			mgInstances = getManagersByClusterId(svc, CLUSTER_ID)
			if len(mgInstances) == 3 {
				break
			}

			fmt.Println("Waiting for instances to be available ...")
			time.Sleep(3 * time.Second)
		}

		shell := make([]ssh.Client, 3)
		// check to private when running as real Operator
		for i, instance := range mgInstances {
			for {
				var err error
				shell[i], err = ssh.NewClient("ubuntu", *instance.PublicIpAddress, 22, &ssh.Auth{
					Keys: Keys,
				})
				err = shell[i].Shell("curl -sSL https://get.docker.com | sudo sh")
				if err == nil {
					break
				}

				fmt.Printf("Waiting for manager [%d] to be SSHable ...\n", i)
				time.Sleep(3 * time.Second)
			}
		}

		shell[0].Shell("sudo docker swarm init --availability=drain --advertise-addr=eth0")
		var err error
		joinInfo, err = getJoinInfo("ubuntu", *mgInstances[0].PublicIpAddress)
		if err != nil {
			panic(err)
		}

		for _, instance := range mgInstances[1:] {
			err := joinAsMaster("ubuntu", *instance.PublicIpAddress, joinInfo)
			if err != nil {
				panic(err)
			}
		}

		for _, sh := range shell {
			sh.Shell("sudo docker plugin install --grant-all-permissions weaveworks/net-plugin:2.1.3")
		}

	} else if mg_count_1st_check == 3 {
		var err error
		fmt.Println("Getting information for joining cluster")
		joinInfo, err = getJoinInfo("ubuntu", *mgInstances[0].PublicIpAddress)
		if err != nil {
			panic(err)
		}

	} else {
		fmt.Println("NYI")
		return
	}

	fmt.Println("Creating a Spot fleet ...")

	userData := fmt.Sprintf(`#!/bin/bash

curl -sSL https://get.docker.com | sh
service docker start
usermod -aG docker ubuntu
docker swarm join --token %s %s
docker plugin install --grant-all-permissions weaveworks/net-plugin:2.1.3
`, joinInfo.workerToken, joinInfo.remoteManagers[0].Addr)

	userDataBase64 := base64.StdEncoding.EncodeToString([]byte(userData))

	result, err := svc.RequestSpotFleet(&ec2.RequestSpotFleetInput{
		SpotFleetRequestConfig: &ec2.SpotFleetRequestConfigData{
			LaunchSpecifications: []*ec2.SpotFleetLaunchSpecification{
				{
					ImageId:      aws.String("ami-325d2e4e"),
					InstanceType: aws.String("m3.medium"),
					KeyName:      aws.String("chanwit-3558"),
					SpotPrice:    aws.String("0.0098"),
					BlockDeviceMappings: []*ec2.BlockDeviceMapping{
						{
							DeviceName: aws.String("/dev/sda1"),
							Ebs: &ec2.EbsBlockDevice{
								DeleteOnTermination: aws.Bool(true),
								VolumeSize:          aws.Int64(8),
							},
						},
					},
					TagSpecifications: []*ec2.SpotFleetTagSpecification{
						{
							ResourceType: aws.String("instance"),
							Tags: []*ec2.Tag{
								{
									Key:   aws.String("ClusterID"),
									Value: aws.String(CLUSTER_ID), // TODO
								},
								{
									Key:   aws.String("Type"),
									Value: aws.String("worker"), // TODO
								},
							},
						},
					},
					UserData: aws.String(userDataBase64),
				},
			},
			IamFleetRole:                     aws.String("arn:aws:iam::738066107778:role/aws-ec2-spot-fleet-tagging-role"),
			TargetCapacity:                   aws.Int64(5),
			TerminateInstancesWithExpiration: aws.Bool(true),
			ValidUntil:                       aws.Time(time.Now().AddDate(1, 0, 0)),
		},
	})

	if err != nil {
		fmt.Println(err)
		fmt.Println(result)
	} else {
		fmt.Println(result)
	}

	// check swarm
	// check docker info on each node

	/*
		switch {
			case mgInstanceCount == 0:
				fmt.Println("Create 1, then 2")
			case mgInstanceCount == 3:
				fmt.Println("OK, doing something next")
			case mgInstanceCount > 0:

		}

		if err != nil {
			panic(err)
		}
	*/

}
