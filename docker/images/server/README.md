#What is BigMemoryMax / Terracotta Server ?

BigMemory Max supports a distributed in-memory data-storage topology, which enables the sharing of data among multiple 
caches and in-memory data stores in multiple JVMs. It uses a Terracotta Server Array to manage data that is shared by 
multiple application nodes in a cluster.

As with Ehcache, you can use BigMemory Max as a general-purpose cache/in-memory data store or a second-level cache for 
Hibernate. You can additionally integrate it with third-party products such as ColdFusion, Google App Engine, and Spring.

##Before proceeding, make sure to

* copy a valid __terracotta-license.key__ to the Terracotta folder (that could be /opt/softwareag/Terracotta for example)


#How to build this image

Once Docker is up and running in your environment, from the root folder (/opt/softwareag for example) run this command :

    export VERSION=4.3.8
    docker build --file docker/images/server/Dockerfile --tag terracotta:$VERSION .

#How to use this image: QuickStart

From the server directory

- Using external configuration and license
    - Create <config-directory> having `tc-config.xml` file
    - Create <license-directory> having `license.key` file

  docker run -d -p 9510:9510 -v <config-directory>:/configs -v <license-directory>:/license --name terracotta terracotta

  docker run -d -p 9510:9510 -e ACCEPT_EULA=Y -e TC_SERVER1=terracotta-1 -e TC_SERVER2=terracotta-2 
              -v /path/to/config-folder-server1:/configs/ 
              -v /path/to/license-folder:/licenses/ 
              -h terracotta-1 
              --name terracotta-1 terracotta:4.3.8

  docker run -d -p 9510:9510 -e ACCEPT_EULA=Y -e TC_SERVER1=terracotta-1 -e TC_SERVER2=terracotta-2
              -v /path/to/config-folder-server2:/configs/
              -v /path/to/license-folder:/licenses/
              -h terracotta-2
              --name terracotta-2 terracotta:4.3.8


At this point go to http://localhost:9510/config from the host machine to see the configuration of your Terracotta Server Array.
It's ready and waiting for clients

##Networking

The Terracotta Server Array is a "static" cluster : it must know before starting its topology (you can not dynamically add new nodes, active or mirror, to the cluster)

All the TSA nodes share the same configuration, so before you start your cluster, you must know the hostnames of each node.

##Volumes

The provided Terracotta Server configuration files will make the containers write all persistent and node-specific data 
under the directory /terracotta/data. As this directory is declared to be a Docker volume, it will be persisted outside 
the normal union filesystem. This results in improved performance.

By default, the persisted location of the volume on your Docker host will be hidden away in a location managed by the 
Docker daemon. In order to control its location - in particular, to ensure that it is on a partition with sufficient 
disk space for your server - we recommend mapping the volume to a specific directory on the host filesystem using 
the -v option to docker run.

#Common Deployment Scenarios
## Single host, single container

    ┌───────────────────────┐
    │   Host OS (Linux)     │
    │  ┌─────────────────┐  │
    │  │  Container OS   │  │
    │  │    (CentOS)     │  │
    │  │  ┌───────────┐  │  │
    │  │  │ Terracotta│  │  │
    │  │  │  Server   │  │  │
    │  │  └───────────┘  │  │
    │  └─────────────────┘  │
    └───────────────────────┘

This is a quick way to try out the Terracotta Server on your own machine with no installation overhead - just download and run. In this case, any networking configuration will work; the only real requirement is that port 9510 be exposed so that the clients can connect to the TSA.

###Verify container start

Use the container name you specified (eg. tsa) to view the logs:

    $ docker logs terracotta

> 2015-06-04 16:11:26,884 INFO - Terracotta Server instance has started up as ACTIVE node on 0:0:0:0:0:0:0:0:9510 successfully, and is now ready for work.

###Grab the config file

From the host, connect your browser to http://localhost:9510/config, and you should get the configuration file.
