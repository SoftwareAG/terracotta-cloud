#!/usr/bin/env bash
# Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
# Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.

# verbose output
set -x

function random_string() {
    echo $(LC_CTYPE=C tr -dc a-z < /dev/urandom | fold -w ${1:-32} | head -n 1)
}

function header() {
    echo "###################################################################################"
    echo "               $1                "
    echo "###################################################################################"
}

function validate_result() {
    command=$1
    processor=$2
    result=$3
    error=$4
    error_action=$5

    for i in {1..5}; do
        result_resp=$(${command} | ${processor} | grep "${result}")

        # If found the container is properly started then return.
        if [ ! -z "$result_resp" ]; then

            # Enducing extra delay before returning
            sleep 10

            return
        fi

        # Induce delay in checking the status.
        sleep 10
    done

    echo "$error"
    $error_action
    exit 10
}


function container_running() {
    container_id=$1
    validate_result "docker inspect $container_id" "jq --raw-output .[0].State.Status" "running" "couldn't have container '$container_id' to running status." "docker logs $container_id"
}