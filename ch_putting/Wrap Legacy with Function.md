# Wrap Legacy with Function

We demonstrate to write a wrapper function for a legacy Web-based system.

To achieve this, we use the Chromeless library to connect to a headless Chrome instance. Then the Chromeless script drives the Chrome browser to do the rest for us.

Here's the diagram of this part of the system.

[diagram of Bank 1]

What chromless doing?

configuration / use local with `chrome` name.

written in NodeJS.

```
chromeless code
```

Prepare chromeless container
how to build it

```
Dockfile
```

We then prepare a function
On the OpenFaaS UI, define a new function and the dialog will allow us to attach the new function to a specific network.

[capture OpenFaaS UI]

We start a headless Chrome instance, exposing it as `chrome` on the same network as the caller function.

```
docker run
```

We start an ERP system. It is the HiveMind ERP from built using Moqui framework. We can download it from the Moqui repository on Github. The Moqui team also prepares a Docker image for use. So just run it and attach it into the main parse_net. 

```
docker run
```

[capture screen of HiveMind]


## calling chain

Starting from the Bank Routing, lookup, call FaaS, call wrapper, call Chrome, call HiveMind.
