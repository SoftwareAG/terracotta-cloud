### Docker image definition for Terracotta Server

#### Introduction

The Terracotta 10.x EE offering includes the following:

 *  Ehcache 3.x compatibility
 *  Terracotta Store compatibility
 *  Distributed In-Memory Data Management with fault-tolerance via Terracotta Server (1 stripe – active with optional mirror)
 *  In memory off-heap storage - take advantage of all the RAM in your server


#### How to start your Terracotta Server(s) in Docker containers

##### Quick start : one active node

    docker run -e ACCEPT_EULA=Y --name terracotta --hostname terracotta -p 9410:9410 -d terracotta-server:10.11

A quick look at the logs :

    docker logs -f terracotta

Should return some logs ending with :

    [Server Startup Thread] INFO com.tc.server.TCServer - Server started as default-node1

It's now ready to be configured using config-tool !

#### How to build this image

To build this Dockerfile

    $ cd terracotta-10.11.0-SNAPSHOT
    $ docker build --file docker/images/server/Dockerfile --tag terracotta-server:10.11.0-SNAPSHOT .