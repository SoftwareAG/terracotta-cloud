# SAG Registry based orchestration files

If you don't want to build the images yourself, fear not, we got you covered !

You should be able to browse them at https://dtr.eur.ad.sag:4443/orgs/terracotta/repos

We're providing you in this folder few deployment scenarios, just launch them from your terminal :


## Run a caching client, a tcstore client and a TMC, all connected to a Terracotta Server

    docker stack deploy terracotta --compose-file docker/orchestration/sag_registry_based/n_clients_1_tc_server_1_tmc.yml

More info at : docker/orchestration/n_clients_1_tc_server_1_tmc/README.md

## Run a 2x2 cluster, with tcstore and caching clients, and a TMC

    docker stack deploy terracotta-2x2 --compose-file docker/orchestration/sag_registry_based/n_clients_2_stripes_with_2_nodes_each_1_tmc.yml

More info at : docker/orchestration/n_clients_2_stripes_with_2_nodes_each_1_tmc/README.md

## don't want to deploy them yourself ?

Once again, we got you covered, hit the [tc deployer](http://tc-docker-swarm-02.eur.ad.sag:4000/) URL and click deploy !
