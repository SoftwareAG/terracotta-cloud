#!/usr/bin/env bash
# Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
# Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.

# verbose output
set -x

source "util.sh"

function cleanup {
  ./smoke-test-cleanup.sh
}

# Cleaning up the enviornment before starting the script.
cleanup

# Cleaning up the environment on exit of this script.
trap cleanup EXIT

# Expecting terracotta VERSION to be passed in as environment variable.
version="$VERSION"

# Ensuring the kit version env variable is passed in.
if [ -z "$version" ]; then
    echo "VERSION environment variable is not provided."
    exit 1
fi


header "Starting Terracotta Server"
terracotta_id=$(docker run -e ACCEPT_EULA=Y --name terracotta --hostname terracotta -p 9410:9410 -d terracotta-server:$version)

# Ensure the terracotta server container started.
container_running "$terracotta_id"
validate_result "docker logs $terracotta_id" "cat" "Server started as default-node1" "terracotta server couldn't properly started"



header "Activating Terracotta Cluster using config-tool"
docker run -i -e ACCEPT_EULA=Y -e LICENSE_URL=https://iwiki.eur.ad.sag/download/attachments/492808213/Terracotta-10.5-linux-unlimited.xml?api=v2 --name config-tool --link terracotta:terracotta terracotta-config-tool:$version activate -l /licenses/license.xml -n "tc-cluster" -s "terracotta"

# Checking is the cluster is properly configured.
if [ "$(echo $?)" != '0' ]; then
    echo "Failed to configure the terracotta cluster"
    exit 11
fi




header "Starting TMC Server"
tmc_id=$(docker run -d -e ACCEPT_EULA=Y -e TMS_DEFAULTURL=terracotta://terracotta:9410 --name tmc --link terracotta:terracotta -p 19480:9480 tmc:$version)

# Ensure the tmc container started.
container_running "$tmc_id"
validate_result "docker logs $tmc_id" "cat" "Undertow started" "couldn't start tmc."




header "Validating the created cluster"
# Ensuring TMC is able to proble the cluster.
validate_result "curl http://localhost:19480/api/connections/probe?uri=terracotta%3A%2F%2Fterracotta%3A9410" "jq --raw-output .connectionName" "tc-cluster" "Cluster is not accessible from TMC."


# Ensuring TMC is able to connect with the cluster
probe_json=$(curl http://localhost:19480/api/connections/probe?uri=terracotta%3A%2F%2Fterracotta%3A9410)
connection_json=$(curl -H "Content-Type: application/json" -X POST -d "$probe_json" http://localhost:19480/api/connections)
offline_reason=$(echo "$connection_json" | jq --raw-output '.status.offlineReason')
if [ "$offline_reason" != "Connection is up and rocking!" ]; then
    echo "Couldn't connect with the cluster from TMC."
    exit 12
fi


header "Validating Ehcache client"
ehcache_client_id=$(docker run -d -e ACCEPT_EULA=Y -e TERRACOTTA_SERVER_URL=terracotta:9410 --name ehcache-client --link terracotta:terracotta sample-ehcache-client:$version)

# Ensure the ehcache client container started.
container_running "$ehcache_client_id"

# Checking ehcache client reflected in tmc.
validate_result "curl http://localhost:19480/api/connections" 'jq --raw-output ."tc-cluster".runtime.ehcacheServerEntities.MyCacheManager.resourcePools."my-resource-pool".offheapResource' "offheap-1" "ehcache client couldn't connect to the cluster"





header "Validating TCStore client"
store_client_id=$(docker run -d -e ACCEPT_EULA=Y -e TERRACOTTA_SERVER_URL=terracotta:9410 --name store-client --link terracotta:terracotta sample-tcstore-client:$version)

# Ensure the store client container started.
container_running "$store_client_id"

# Checking store client is reflected in tmc.
validate_result "curl http://localhost:19480/api/connections" 'jq --raw-output ."tc-cluster".runtime.datasetServerEntities."MyDataset-1".offheapResourceName' "offheap-2" "store client couldn't connect to the cluster"
