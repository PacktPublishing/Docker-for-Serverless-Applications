package main

type A interface {
	Create()
	Update()
	Remove()
}


func TypeNode(a A) {

}

func main() {

	TypeNode(interface{}{
		func Create() {

		}
	})

}