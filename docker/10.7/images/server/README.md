### Docker image definition for Terracotta Server

#### Introduction

The Terracotta 10.x EE offering includes the following:

 *  Ehcache 3.x compatibility
 *  Terracotta Store compatibility
 *  Distributed In-Memory Data Management with fault-tolerance via Terracotta Server (1 stripe – active with optional mirror)
 *  In memory off-heap storage - take advantage of all the RAM in your server


#### How to start your Terracotta Server(s) in Docker containers

##### Quick start : one active node

    docker run -e ACCEPT_EULA=Y --name terracotta -p 9410:9410 -d terracotta-server:10.7.0-SNAPSHOT

A quick look at the logs :

    docker logs -f tc-server

Should return some logs ending with :

    [TC] 2017-03-22 03:39:26,627 INFO - Terracotta Server instance has started up as ACTIVE node on 0.0.0.0:9410 successfully, and is now ready for work.

It's now ready and waiting for clients !

#### How to build this image

To build this Dockerfile

    $ cd terracotta-10.7.0-SNAPSHOT
    $ docker build --file docker/images/server/Dockerfile --tag terracotta-server:10.7.0-SNAPSHOT .