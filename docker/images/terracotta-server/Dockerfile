# Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
# Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG
#
# This Dockerfile defines an image that can run as Terracotta Server

FROM iregistry.eur.ad.sag/ibit/ubi7:jdk8u282-ubi7.9-262_approved
LABEL maintainers="Anthony Dahanne <anthony.dahanne@softwareag.com>"

# SAG_HOME is defined to /opt/softwareag in the base image
COPY ./legal $SAG_HOME/legal
ADD https://confluence.terracotta.org/download/attachments/29557169/TDB_10.5_terms.pdf?version=1&modificationDate=1570068076106&api=v2 \
 $SAG_HOME/legal/TDB_10.5_terms.pdf

COPY ./server/ $SAG_HOME/server/
COPY docker/images/terracotta-server/conf/tc-config-single-node.xml $SAG_HOME/server/conf/tc-config-single-node.xml
COPY docker/images/terracotta-server/conf/tc-config-active-passive.xml $SAG_HOME/server/conf/tc-config-active-passive.xml
COPY docker/images/terracotta-server/conf/logback.xml $SAG_HOME/server/lib/logback-test.xml
COPY docker/images/terracotta-server/entrypoint.sh $SAG_HOME/server/entrypoint.sh

# /data/dataroots/ will persist data roots (FRS persistence for example); better not be on container filesystem !
ENV DATAROOTS_DIRECTORY "/data/dataroots/"
VOLUME $DATAROOTS_DIRECTORY
# /data/backups/ will receive backup data; it must go to a highly trustable volume mount !
ENV BACKUPS_DIRECTORY "/data/backups/"
VOLUME $BACKUPS_DIRECTORY
# /configs is an optional volume you can use to deploy configuration files
ENV CONFIGS_DIRECTORY "/configs/"
VOLUME $CONFIGS_DIRECTORY

# all below commands will now be relative to this path
WORKDIR $SAG_HOME/server

# the tsa port (used by the clients to connect to the cluster)
EXPOSE 9410
# the group port (used to sync the passives with the active)
EXPOSE 9430

# default values for offheap, that you can override when starting your container with docker run -e OFFHEAP_RESOURCE1_SIZE=512 for example
ENV OFFHEAP_RESOURCE1_NAME "offheap-1"
ENV OFFHEAP_RESOURCE1_UNIT "MB"
ENV OFFHEAP_RESOURCE1_SIZE "512"

ENV OFFHEAP_RESOURCE2_NAME "offheap-2"
ENV OFFHEAP_RESOURCE2_UNIT "MB"
ENV OFFHEAP_RESOURCE2_SIZE "512"

ENV DATA_RESOURCE1 "dataroot-1"
ENV DATA_RESOURCE2 "dataroot-2"

# make the JVM aware of the container memory constraints, since JDK 8u131
# see https://blog.csanchez.org/2017/05/31/running-a-jvm-in-a-container-without-getting-killed
ENV JAVA_OPTS "-XX:+UnlockExperimentalVMOptions -XX:+UseCGroupMemoryLimitForHeap"

# the entrypoint needs to be run as root while setting up the config; it run the JVM process as sagadmin though
USER root

# before starting the terracotta server, we update the tc-config.xml configuration file
ENTRYPOINT ["./entrypoint.sh"]
