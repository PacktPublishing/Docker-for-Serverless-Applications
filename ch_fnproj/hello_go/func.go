package main

import (
	"encoding/json"
	"fmt"
	"os"
)

type Message struct {
	Name string
}

func main() {
	m := &Message{Name: "World"}

	err := json.NewDecoder(os.Stdin).Decode(m)
	if err != nil {
		fmt.Fprintf(os.Stderr, "err JSON Decode: %s\n", err.Error())
		os.Exit(250)
	}

	fmt.Printf(`{"success": "Hello %s"}`, m.Name);
	os.Exit(0)
}
