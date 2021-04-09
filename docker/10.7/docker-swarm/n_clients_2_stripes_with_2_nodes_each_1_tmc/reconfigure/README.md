# Run a 2x2 cluster, with possibility of reconfigure

If you have not yet the instructions about n clients, 1 tc server, 1 tmc, it is strongly suggested you have a look and run it.

### pre requisites
Build a cluster tool with your configuration files and a license.

    docker build --file docker/orchestration/n_clients_2_stripes_with_2_nodes_each_1_tmc/reconfigure/initial-cluster-tool/Dockerfile --tag cluster-tool-with-configs:10.7.0-SNAPSHOT .

When this image is run, it will copy config files to a configs volume and the license to a licenses volume.


### Orchestration example with Docker stack

If you're using Docker swarm mode, you may want to use docker stack to deploy across all nodes part of your Docker swarm

You can re use the same docker-compose.yml file :

    docker stack deploy terracotta-2x2 --compose-file docker/orchestration/n_clients_2_stripes_with_2_nodes_each_1_tmc/reconfigure/docker-compose.yml
    Creating network terracotta_terracotta-net
    Creating service terracotta_terracotta-1
    Creating service terracotta_terracotta-2
    Creating service terracotta_client

### Reconfigure the cluster

* Update the cluster tool image and config files or licenses (if growing a stripe, also update the compose file)

        docker build --file docker/orchestration/n_clients_2_stripes_with_2_nodes_each_1_tmc/reconfigure/updated-cluster-tool/Dockerfile --tag cluster-tool-with-configs-updated:10.7.0-SNAPSHOT .

* Run a service based on this new cluster tool image :

        docker service create --name reconfigure --restart-condition=none  --network terracotta-2x2_terracotta-net -e "ACCEPT_EULA=Y" --mount source=terracotta-2x2_configs,target=/configs   cluster-tool-with-configs-updated:10.7.0-SNAPSHOT reconfigure -n MyCluster /configs/stripe1.xml /configs/stripe2.xml

* Check everything went fine :

        docker service logs -f reconfigure
        You accepted license agreement
         + su -c 'cluster-tool/bin/cluster-tool.sh reconfigure -n MyCluster -s terracotta-1-1:9410' sagadmin
        Command completed successfully.

* kill the passives in both stripes : (provided terracotta-1-2 and terracotta-2-2 are passive, you can check in the TMC first)

        docker service scale terracotta-2x2_terracotta-1-2=0
        docker service scale terracotta-2x2_terracotta-2-2=0

and then restart them :

        docker service scale terracotta-2x2_terracotta-1-2=1
        docker service scale terracotta-2x2_terracotta-2-2=1

* fail over the 2 stripes now killing the actives (in the present case terracotta-1-1 and terracotta-2-1)

        docker service scale terracotta-2x2_terracotta-1-1=0
        docker service scale terracotta-2x2_terracotta-2-1=0

and then restart them :

        docker service scale terracotta-2x2_terracotta-1-1=1
        docker service scale terracotta-2x2_terracotta-2-1=1

There you go, you reconfigured your cluster !
