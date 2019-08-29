#What is the sample ehcache client ?

Terracotta Server supports a distributed in-memory data-storage topology, which enables the sharing of data among multiple caches and in-memory

The client java app connects to the Terracotta Server and starts putting and getting key/value mappings.

You can have a look at its code at : src/ClientDoingInsertionsAndRetrievals.java ; it's configured to hold few elements (50) on heap, and of course use the Terracotta server at terracotta:9410 as its clustered tier.

The client will either insert or retrieve values every 0.1 seconds


#How to use this image: QuickStart

You can start it up simply with :

    docker run -e ACCEPT_EULA=Y --name client -d sample-ehcache-client:10.3.0-SNAPSHOT

But you would get such an error message :

    The environment variable TERRACOTTA_SERVER_URL was not set; using terracotta:9410 as the cluster url.

followed by :

    WARN - We couldn't load configuration data from the server at 'terracotta:9410'; retrying. (Error: terracotta.)


That would be because you need a Terracotta server running.

You can run a terracotta server using (provided you built the terracotta image) :

    docker run -e ACCEPT_EULA=Y -d -p 9410:9410 --name terracotta terracotta-server:10.3.0-SNAPSHOT

Don't forget to install a license using the cluster tool :

    docker run -e ACCEPT_EULA=Y --link terracotta:terracotta -e "LICENSE_URL=https://server/license.xml" terracotta-cluster-tool:10.3.0-SNAPSHOT configure -n MyCluster -s terracotta

and then re try running the client, with :

    docker run -e ACCEPT_EULA=Y -d --link terracotta:terracotta --name caching-client sample-ehcache-client:10.3.0-SNAPSHOT

and checkout what's happening with :

    docker logs -f caching-client


#### How to build this image

To build this Dockerfile

    $ cd terracotta-db-10.3.0-SNAPSHOT
    $ docker build --file docker/images/sample-ehcache-client/Dockerfile --tag sample-ehcache-client:10.3.0-SNAPSHOT .