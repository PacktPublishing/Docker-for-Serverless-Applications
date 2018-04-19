package lib

import (
	"os"
	"fmt"
	"context"
	"encoding/json"

	jq "github.com/threatgrid/jq-go"

	"github.com/hokaccha/go-prettyjson"
	"github.com/docker/docker/api/types"
	_ "github.com/docker/docker/api/types/filters"
	"github.com/docker/docker/client"
)

type Client struct {
	ctx context.Context
	cli *client.Client
}

func NewClient() *Client {
	ctx := context.Background()
	cli, err := client.NewEnvClient() // client.DefaultDockerHost, "1.30", nil, nil)
	if err != nil {
		panic(err)
	}

	return &Client{
		ctx,
		cli,
	}
}

func Json(o interface{}) string {
	s, err := json.Marshal(o)
	if err != nil {
		panic(err)
	}

	return string(s)
}

func Dump(o interface{}) {
	s, err := prettyjson.Marshal(o)
	if err != nil {
		panic(err)
	}

	fmt.Println(string(s))
}

func Jq(query string, o interface{}) {
	jq.Dump(os.Stdout, query, o)
}

func (c *Client) Info() types.Info {
	info, err := c.cli.Info(c.ctx)
	if err != nil {
		panic(err)
	}

	return info
}
