# Networking

To make all functions of differernt platforms will be able to talk together, we need to setup a proper container networking.

The demo discussing in this chapter is not a simple FaaS example. It is a complex scenario where functions call other functions.

Normally on some Serverless platforms like Lambda, we may sometime assume that all function runs on the flat network of the provider. 

In contrast, when we run functions on our own platforms, we could segment the networks ourselves and function networking will become a challenge. However, networking will be relatively simple because the networking model in Docker and Swarm mostly flat.

How could we achieve?

1. We create an attachable Swarm-scoped network.
2. We start a FaaS framework, make its gateway attach to that network.
3. We also need to tell the framework that it must attach that network to every container it created.

In OpenFaas, it allows to create a function to run on a specific network.
In OpenWhisk, we could specify this with a configuration of an Invoker.
In FN, we need a hack. The version with function networking patch is available at https://github.com/chanwit/fn.

