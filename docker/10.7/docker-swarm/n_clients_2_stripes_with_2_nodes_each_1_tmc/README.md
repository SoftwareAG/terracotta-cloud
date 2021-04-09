# Run a 2x2 cluster, with tcstore and caching clients, and a TMC

If you have not yet the instructions about n clients, 1 tc server, 1 tmc, it is strongly suggested you have a look and run it.

### Orchestration example with Docker stack

Exactly the same as n clients, 1 tc server and 1 tmc

If you're using Docker swarm mode, you may want to use docker stack to deploy across all nodes part of your Docker swarm

You can re use the same docker-compose.yml file :

    docker stack deploy terracotta-2x2 --compose-file docker/orchestration/n_clients_2_stripes_with_2_nodes_each_1_tmc/docker-compose.yml
    Creating network terracotta_terracotta-net
    Creating service terracotta_terracotta-1
    Creating service terracotta_terracotta-2
    Creating service terracotta_client
