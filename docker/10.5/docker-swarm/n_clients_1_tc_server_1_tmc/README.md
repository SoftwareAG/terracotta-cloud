# Run a caching client, a tcstore client and a TMC, all connected to a Terracotta Server

You need to have built three images : client, tmc and terracotta.

Also, make sure you don't have containers already runnning :

    $ docker ps
    CONTAINER ID        IMAGE               COMMAND                  CREATED              STATUS              PORTS                                        NAMES


### Orchestration example with Docker stack

If you're using Docker swarm mode, you may want to use docker stack to deploy across all nodes part of your Docker swarm

You can re use the same docker-compose.yml file :

    docker stack deploy terracotta --compose-file docker/orchestration/n_clients_1_tc_server_1_tmc/docker-compose.yml
    Creating network terracotta_terracotta-net
    Creating service terracotta_terracotta-1
    Creating service terracotta_terracotta-2
    Creating service terracotta_client

Let's have a look at the status

    docker stack ps terracotta
    ID            NAME                     IMAGE                                  NODE  DESIRED STATE  CURRENT STATE           ERROR  PORTS
    dylv0cdeat1v  terracotta_terracotta.1  terracotta-server:10.5.0-SNAPSHOT      moby  Running        Running 40 seconds ago
    aung8zqoisnn  terracotta_tmc.1         tmc:10.5.0-SNAPSHOT                    moby  Running        Running 41 seconds ago
    ifmf04vyb18a  terracotta_caching_client.1      sample-ehcache-client:10.5.0-SNAPSHOT  moby  Running        Running 42 seconds ago

You want to scale, right ?

    docker service scale terracotta_caching_client=4
    terracotta_caching_client scaled to 4

Wondering what those services are up to ?

    docker service logs -f terracotta_caching-client

You can enjoy the TMC, load this url : http://DOCKER_HOST:PUBLISHED_PORT

where you can find PUBLISHED_PORT doing :

    docker service inspect terracotta_tmc | grep PublishedPort
                        "PublishedPort": 30000,

Finally, you can tear down your deployment :

    docker stack rm terracotta
    Removing service terracotta_caching-client
    Removing service terracotta_tmc
    Removing service terracotta_store-client
    Removing service terracotta_terracotta
    Removing network terracotta_terracotta-net
