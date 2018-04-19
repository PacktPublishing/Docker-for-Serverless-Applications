package main

import (
	"math"
	"fmt"

	gv "gopkg.in/Knetic/govaluate.v2"
)

func main() {
	expr, err := gv.NewEvaluableExpression("blue * (1/3)");
	if err != nil {
		panic(err)
	}

	params := make(map[string]interface{}, 8)
	params["blue"] = 10;

	result, err := expr.Evaluate(params);
	if err != nil {
		panic(err)
	}

	replicas := int(math.Ceil(result.(float64)))
	fmt.Println(replicas)
}