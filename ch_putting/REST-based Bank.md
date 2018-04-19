# REST-based Bank

Beside writing a simple processor, the technique in this section is one of the simplest forms of using Functions. We have a bank backend with REST APIs exposed. So we write a Function as a *glue* to hide the complex interface of the backend. In this example, we use Go as the language to implement the Function.


The scenario is that we have a REST API server and we want to unify it with another similar service. In the example of this chapter, we have 2 banking backends with different ways of interaction. The first one is a Web-based UI without REST interface, another one is the REST API in this section.

```
go code
```

here how we build.
we use the multi stage build technique.

```
docker file 
multi stage
```

Do not forget to push the image onto Docker Hub before proceed to the next step.

Then we define the function using wsk cli command.

```  
wsk -i action create --image
```

before start OpenWhisk, we need to change the invoker configuration to start every container inside the parse_net network.

```
change config in docker compose
``` 

To unify, we create a Function to wrap the REST API and make both of interfaces as similar as possible.

[function w/ go] -> [REST API] 

# test with unit test

```
WHISK_SERVICE=http://..... gradle test <test spec>
```

# calling chain and how we use it.