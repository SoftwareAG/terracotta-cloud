#!/bin/bash
# Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
# Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.

if [ "$ACCEPT_EULA" == "Y" ]; then
    echo "You accepted license agreement"
else
    echo "You must accept license agreement with ACCEPT_EULA=Y environment variable. Exiting..."
    exit 10
fi

# Run cluster-tool, passing all the passed in container arguments to the cluster-tool.sh script.
CMD="bin/cluster-tool.sh $@"

# It will help in identifying what command ran in the container.
set -x

su -c "$CMD" sagadmin

# from cluster-tool help :
#Exit Codes:
#    SUCCESS(0)
#    PARTIAL_FAILURE(1)
#    FAILURE(2)
#    ALREADY_CONFIGURED(28)
#    BAD_REQUEST(40)
#    NOT_FOUND(44)
#    SECURITY_CONFLICT(48)
#    CONFLICT(49)
#    INTERNAL_ERROR(50)
#    NOT_IMPLEMENTED(51)
#    BAD_GATEWAY(52)