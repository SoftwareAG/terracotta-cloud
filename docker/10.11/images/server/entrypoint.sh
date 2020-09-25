#!/bin/bash
# Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
# Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.

if [ "$ACCEPT_EULA" == "Y" ]; then
    echo "You accepted license agreement"
else
    echo "You must accept license agreement with ACCEPT_EULA=Y environment variable. Exiting..."
    exit 10
fi

sed -i -r 's/OFFHEAP_RESOURCE1_NAME/'$OFFHEAP_RESOURCE1_NAME'/; s/OFFHEAP_RESOURCE1_UNIT/'$OFFHEAP_RESOURCE1_UNIT'/; s/OFFHEAP_RESOURCE1_SIZE/'$OFFHEAP_RESOURCE1_SIZE'/' conf/tc-cluster-single-node.properties \
  && sed -i -r 's/OFFHEAP_RESOURCE2_NAME/'$OFFHEAP_RESOURCE2_NAME'/; s/OFFHEAP_RESOURCE2_UNIT/'$OFFHEAP_RESOURCE2_UNIT'/; s/OFFHEAP_RESOURCE2_SIZE/'$OFFHEAP_RESOURCE2_SIZE'/' conf/tc-cluster-single-node.properties \
  && sed -i -r 's/DATA_RESOURCE1/'$DATA_RESOURCE1'/g; s/DATA_RESOURCE2/'$DATA_RESOURCE2'/g' conf/tc-cluster-single-node.properties \
  && sed -i -r 's/HOSTNAME/'$HOSTNAME'/g' conf/tc-cluster-single-node.properties \

# chown in a volume can be problematic : if the volume is a nfs mount, maybe root can't (and don't need to) chown it for example...
su sagadmin -c "test -w $BACKUPS_DIRECTORY" || (echo "$BACKUPS_DIRECTORY not writeable by sagadmin, trying to chown it" && chown -R sagadmin:sagadmin $BACKUPS_DIRECTORY)
su sagadmin -c "test -w $DATAROOTS_DIRECTORY" || (echo "$DATAROOTS_DIRECTORY not writeable by sagadmin, trying to chown it" && chown -R sagadmin:sagadmin $DATAROOTS_DIRECTORY)

su -c "bin/start-tc-server.sh -f conf/tc-cluster-single-node.properties" sagadmin;