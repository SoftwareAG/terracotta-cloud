#!/bin/bash
# Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
# Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.

if [ "$ACCEPT_EULA" == "Y" ]; then
    echo "You accepted license agreement"
else
    echo "You must accept license agreement with ACCEPT_EULA=Y environment variable. Exiting..."
    exit 10
fi

# chown in a volume can be problematic : if the volume is a nfs mount, maybe root can't (and don't need to) chown it for example...
su sagadmin -c "test -w $TMS_STORAGEFOLDER" || (echo "$TMS_STORAGEFOLDER not writeable by sagadmin, trying to chown it" && chown -R sagadmin:sagadmin $TMS_STORAGEFOLDER)

su -c "bin/start.sh" sagadmin