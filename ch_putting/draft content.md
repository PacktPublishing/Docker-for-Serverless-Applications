draft content


Overall scenario.
We use mobile payment with intra-bank money transfer as the scenario for this chapter. 

Why money transfer?

First, the logic is easy to understand. So we do not need to worry about the complexity of its business logics. Transfering money from one to another. Just that simple. Then we can focus on the complexity of the architecture.

Money transfer is easy, but inter-bank transfer is hard.

THe reason of its hardship is that we could not directly apply the concept of transaction to any external system.

Here's the scenario.



=======

what is parse?
Similar to Firebase, Parse is a backend as a service platform.

With Parse, developers do not need to code the backend system them selves.

Parse is basically used by mobile application developers to help accellerate the development process. Together with the dashboard system, they provide an easy to use UI to craft all data entities (called classes) need to help process business logics.

The Parse platform provides an extensible mechanism to allow us to process business logic externally. This is where Functions came in. This mechanism is called WebHook.

We have Functions running as external processes and uses it in conjuction with the Parse WebHook.

For example, in this chapter, we define class Transfer.
Then we define a WebHook for this class to call the external Function everytime before each Transfer entity is saved.

This external Function is Bank Routing written in Java, deployed into FN.

The data sent from Parse is encoded as JSON and will be there in the Bank Routing Function as an input String.

We then unmarshal the input into an object.

The important information are
from
to
objectId
amount

and we have a switch called `sent`.

and status called `processed`.

because an entity will be processed by the WebHook function everytime its field is being changed. So we design that the entity will be processed only when the switch sent true.

if the switch is null or false the Transfer entity will be just allowed to update but not further processed.

what does it mean by processed?

first the from telephone number will be looked up to be bank name and account.

this mapping information is stored in side the blockchain.

the to telephone number is then looked up to obtain the destination bank and account.

if both looking up are successful, the transaction will be marked Started.

this status change is done into the blockchain as well. Also the event TransactionStarted will be emit so external programs listening to the event could observe changes of the overall system.

=======

Blockchain smart contracts

there are two categories of smart contracts in this system.

one of them is the Entity category.
another one is the implementation of the Repository pattern.

Registration and RegistrationRepository

TransferState and TransferStateRepository.

There are 3 states in this system.
First Started.
Second Pending,
and Third Completed.

When the source and the destination numbers are looked up successfully, the state of transfer is set to be STARTED.

When the source account is deducted, the state is changedd to PENDING.

and when money is deposit into the destination account, the state is changed to COMPLETED.

An ethereum client is started on the same network as other components of the system. It appears as `blockchain` service with port 8545.

RegistrationRepository.deployed()
repo.register("faas", "")
repo.register("whisk","")


[chart of accouts]
[mapping] => [accounts]

Smart contracts are written in Solidity. Its initialization scripts are in JavaScript.

We generate using Web3j.

Deploy smart contracts on blockchain.

- deploy


=======================

Bank Routing Function

Bank Routing Function is implemented in Java. It is responsible for looking up a bank account and destination from a mobile number. Then we use the bank account information for money transfer. The routing function uses a set of smart contracts to keep state of each transfer transaction. In this scenario, we deploy the routing function on FN. The function then has to talk to other Functions implemented on other platforms, OpenFaaS and OpenWhisk.

All smart contracts ABI are generated using web3j, the Ethereum Web3 implementation for Java
Client connecting to OpenFaaS is OkHttp.

Client connecting to OpenWhisk is Swagger based client wrapping around OkHttp. Its the same client used by SuraWhisk UI mentioned in Chapter x.

Test case written as an executable specification.

==============

Legacy Bank

A usual use case for Function is a to use it as a wrapper for the old legacy system.

Here's the scenario.

A bank was implemented as a Web-based application and does not have the REST endpoint. We implement a Function with Chromless framework written in NodeJS to talk to a headless Chrome instance, our familiar Web browser. We drive the Chrome instance with scripting to control the legacy Web application for us.

Here's the diagram what we are going to do.

[function + chromeless] -> [headless chrome] -> [legacy Web app]

==============

REST-based Bank

Beside writing a simple processor, the technique in this section is one of the simplest forms of using Functions. We have a bank backend with REST APIs exposed. So we write a Function as a *glue* to hide the complex interface of the backend. In this example, we use Go as the language to implement the Function.


The scenario is that we have a REST API server and we want to unify it with another similar service. In the example of this chapter, we have 2 banking backends with different ways of interaction. The first one is a Web-based UI without REST interface, another one is the REST API in this section.

To unify them, we create a Function to wrap the REST API and make both of interfaces as similar as possible.

[function w/ go] -> [REST API] 

==============

Streaming Agent

Another use case for Function is to use its as a processor for streaming,

We implement a streaming agent using an event-based Java library with RxJava.
Web3J already have RxJava observable to receieve streaming data from an Etereum blockchain.

Here's what we going to do.

[blockchain] -> [streaming agent] -> [function] ---
1. Parse
2. Minio

[function notify many backends]

We run the agent as a container on the same network as the blockchain.

The stream of events coming into the agent.
We use agent to divert each transaction information to other endpoints. In this example, we have 2 endpoints. The first one is the record inside Parse. Another one is the S3 compatible storage, Minio. We upload a file to Minio when the transaction is completed.

======

FN project connects
