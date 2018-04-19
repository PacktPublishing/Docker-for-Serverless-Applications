# Stream Processor

Another use case of Function is to use its as a processor for data stream.

A stream may be sent out from any kind of sources, such as data buses or event buses.

Kafa, Twitter, or blockchain in our case, Ethereum, is a source of data streaming. An Ethereum blockchain could emit events specific to some smart contracts when certain actions is taken.

To observe these events in the form of data stream, we need to use a kind of reactive client. RxJava is one of them.

We implement a streaming agent using an event-based Java library with RxJava. Web3J, the Ethereum client we are using, already have RxJava observables to receieve streaming data from an Etereum blockchain.

Here's what we going to do.

[blockchain] -> [streaming agent] 
    -----> [f(x) stream processor]
    +
    -----> [parse]

[function notify many backends]

We run the agent as a container on the same network as the blockchain.

The stream of events coming into the agent.
We use agent to divert information of each transaction to other endpoints. In this example, we have 2 endpoints. The first one is the record inside Parse. Another one will be talking to a Function, then the function will write data to a S3 compatible storage, Minio.

```
agent codes
```

## how to build agent

## how to deploy agent

## agent in action


Stream Processor funnction

```
function code
```

How to build

How to deploy


