#!/bin/sh
# Copyright (c) 2011-2020 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
# Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.

###############################################################################
# Hack the container from within.
# Sets up an IPv6 only network (unless "keep4" is the param)
# Then uses the volume mounted at /ipv6stuff to share
# hosts file content between machines, emulating DNS
#
# All of this only works in containers with the NET_ADMIN capability
###############################################################################

echo "PARAMS to $0 are $*"

if [ "$1" = "keep4" ] ; then
	echo "Keeping IPV4 address"
else
    echo "Removing IPv4 address"
    ifconfig eth0 0.0.0.0
	# Remove existing ipv4 line from /etc/hosts
	# Docker does not allow removal of this file and thus sed -i isn't going to work.
	cp /etc/hosts /tmp/1 ; sed -i "s/^.*\s$HOSTNAME//" /tmp/1 ; cat /tmp/1 > /etc/hosts
fi


IP6=$(ip addr show eth0 | grep inet6 | grep global | awk '{print $2}' | cut -d / -f 1)
echo "Host $HOSTNAME has ipv6 ip $IP6"

# export our IP
echo "$IP6   $HOSTNAME" > /ipv6stuff/hosts.$HOSTNAME

sleep 2 # Wait for all containers to reach this point

# Assemble all entries
cat /ipv6stuff/hosts.* >> /etc/hosts
echo "$HOSTNAME: My hosts file:"
cat /etc/hosts

set -e

# check basic communication, hardcoded:
ping6 -c 1 terracotta
ping6 -c 1 client

# quick check that network interfaces can be identified from java:
cd /
ifconfig
javac Test.java
java Test $HOSTNAME

