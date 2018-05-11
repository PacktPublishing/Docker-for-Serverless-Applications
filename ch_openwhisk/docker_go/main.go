package main

import (
  "encoding/json"
  "fmt"
  "os"
)

func main() {
  rawParams := []byte(os.Args[1])
  params := map[string]string{}

  err := json.Unmarshal(rawParams, &params)
  if err != nil {
    fmt.Printf(`{"error":%q}`, err.Error())
    os.Exit(0)
  }

  keys := []string{}
  values := []string{}
  for k, v := range params {
    keys = append(keys, k)
    values = append(values, v)
  }

  result := map[string]interface{}{
    "message": "Hello from Go",
    "keys":    keys,
    "values":  values,
  }

  rawResult, err := json.Marshal(result)
  if err != nil {
    fmt.Printf(`{"error":%q}`, err.Error())
    os.Exit(0)
  }

  fmt.Print(string(rawResult))
}
