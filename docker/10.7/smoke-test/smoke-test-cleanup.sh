#!/usr/bin/env bash
# Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
# Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.

# verbose output
set -x

source "util.sh"

header "Stop and Remove all containers"
docker rm -f terracotta tmc store-client ehcache-client config-tool