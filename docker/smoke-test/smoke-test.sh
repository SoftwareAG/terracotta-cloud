#!/usr/bin/env bash
# Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
# Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.

# verbose output
set -x

source "util.sh"

function cleanup {
   cd docker/smoke-test	
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

cd ../..

mkdir config-1
mkdir config-2
mkdir license

#allow access to docker user to create files in config directory which will be mounted
#this is just for example, please provide only enough premissions while deploying in production
chmod 777 config-1 config-2

cp docker/images/bigmemorymax-server/config/tc-config.xml config-1
cp docker/images/bigmemorymax-server/config/tc-config.xml config-2
cp terracotta-license.key license
mv license/terracotta-license.key license/license.key

abs_path=$(pwd)

echo $abs_path

header "Starting Terracotta Servers"
terracotta1_id=$(docker run -d -p 9510:9510 -p 9530:9530 -p 9540:9540 -e ACCEPT_EULA=Y -e TC_SERVER1=terracotta-1 -e TC_SERVER2=terracotta-2 \
            -v "$abs_path/config-1":/configs/ -v "$abs_path/license":/licenses/ \
            -h terracotta-1 --name terracotta-1 bigmemorymax-server:$version)

container_running "$terracotta1_id"
validate_result "docker logs $terracotta1_id" "cat" "ACTIVE-COORDINATOR" "terracotta server couldn't properly started"

terracotta2_id=$(docker run -d -p 9610:9510 -p 9630:9530 -p 9640:9540 -e ACCEPT_EULA=Y -e TC_SERVER1=terracotta-1 -e TC_SERVER2=terracotta-2 \
              -v "$abs_path/config-2":/configs/ -v "$abs_path/license":/licenses/ \
              -h terracotta-2 --name terracotta-2 --link terracotta-1:terracotta-1 bigmemorymax-server:$version)

container_running "$terracotta2_id"

docker rm -f $terracotta1_id

terracotta1_id=$(docker run -d -p 9510:9510 -p 9530:9530 -p 9540:9540 -e ACCEPT_EULA=Y -e TC_SERVER1=terracotta-1 -e TC_SERVER2=terracotta-2 \
              -v "$abs_path/config-1":/configs/ -v "$abs_path/license":/licenses/ \
              -h terracotta-1 --name terracotta-1 --link terracotta-2:terracotta-2 bigmemorymax-server:$version)

validate_result "docker logs $terracotta2_id" "cat" "ACTIVE-COORDINATOR" "terracotta server couldn't properly started"

header "Starting TMC Server"
tmc_id=$(docker run -d -p 9889:9889 -e ACCEPT_EULA=Y -v "$abs_path/license":/licenses --name tmc --link terracotta-1:terracotta-1 --link terracotta-2:terracotta-2 bigmemorymax-tmc:$version)

# Ensure the tmc container started.
container_running "$tmc_id"

header "Validating Ehcache client"
ehcache_client_id=$(docker run -d -e ACCEPT_EULA=Y -v "$abs_path/license":/licenses/ --link terracotta-2:terracotta-2 --link terracotta-1:terracotta-1 --name ehcache-client bigmemorymax-ehcache-client:$version)

# Ensure the ehcache client container started.
container_running "$ehcache_client_id"
validate_result "docker logs $ehcache_client_id" "cat" "Getting" "ehcache client not working"
