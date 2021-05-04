#!/bin/bash
# Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
# Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
if [ "$ACCEPT_EULA" == "Y" ]; then
    echo "You accepted license agreement"
else
    echo "You must accept license agreement with ACCEPT_EULA=Y environment variable. Exiting..."
    exit 10
fi

if [ ! -f "$LICENSE_DIRECTORY/license.key" ]; then
    echo "license file missing at $LICENSE_DIRECTORY/license.key"
    exit 30
fi

# Changing permission for sagadmin home directory.
chown -R sagadmin:sagadmin $SAG_HOME

su -c "java $JAVA_OPTS ClientDoingInsertionsAndRetrievals" sagadmin;