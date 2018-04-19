package main

import (
	"fmt"
	"os"
	"net/http"
	"bytes"

	"encoding/json"
)

type Entry struct {
	Account `json:"account"`
	Amount  float64 `json:"amount"`
}

type Account struct {
	Id string `json:"id"`
}

func main() {
	input := os.Args[1]

	// OpenWhisk params are key/value paris
	params := map[string]interface{}{}
	err := json.Unmarshal([]byte(input), &params)
	if err != nil {
		fmt.Printf(`{"error":"%s", "input": "%s"}`, err.Error(), string(input))
		os.Exit(-1)
	}

	entry := Entry{
		Account: Account{
			Id: params["accountId"].(string),
		},
		Amount: params["amount"].(float64),
	}

	jsonValue, err := json.Marshal(entry)
	if err != nil {
		fmt.Printf(`{"error":"%s"}`, err.Error())
		os.Exit(-1)
	}

	accountService := os.Getenv("ACCOUNT_SERVICE")
	if accountService == "" {
		accountService = "http://accounting:8080/entries"
	}

	resp, err := http.Post(accountService, "application/json", bytes.NewBuffer(jsonValue))

	if err != nil {
		fmt.Printf(`{"error":"%s"}`, err.Error())
		os.Exit(-1)
	}

	if resp.StatusCode >= 200 && resp.StatusCode <= 299 {
		fmt.Println(`{"success": "ok"}`)
		os.Exit(0)
	}

	fmt.Printf(`{"error": "%s"}`, resp.Status)
}
