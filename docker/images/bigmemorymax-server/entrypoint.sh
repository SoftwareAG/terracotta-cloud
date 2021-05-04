#!/bin/bash
# Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
# Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.

if [ "$ACCEPT_EULA" == "Y" ]; then
    echo "You accepted license agreement"
else
    echo "You must accept license agreement with ACCEPT_EULA=Y environment variable. Exiting..."
    exit 10
fi

if [ ! -f "$CONFIGS_DIRECTORY/tc-config.xml" ]; then
    echo "config file missing at $CONFIGS_DIRECTORY/tc-config.xml"
    exit 20
fi

if [ ! -f "$LICENSE_DIRECTORY/license.key" ]; then
    echo "license file missing at $LICENSE_DIRECTORY/license.key"
    exit 30
fi

# Replacing the value of OFFHEAP_ENABLED and OFFHEAP_MAX_SIZE as passed in via environment variable.
sed -i -r 's/OFFHEAP_SIZE/'$OFFHEAP_SIZE'/' $CONFIGS_DIRECTORY/tc-config.xml \
  && sed -i -r 's/TC_SERVER1/'$TC_SERVER1'/g; s/TC_SERVER2/'$TC_SERVER2'/g' $CONFIGS_DIRECTORY/tc-config.xml

# Changing permission for sagadmin home directory.
chown -R sagadmin:sagadmin $SAG_HOME

su -c "bin/start-tc-server.sh -f $CONFIGS_DIRECTORY/tc-config.xml -n $HOSTNAME" sagadmin;