# Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
# Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG

FROM iregistry.eur.ad.sag/ibit/ubi7:jdk8u282-ubi7.9-262_approved
LABEL maintainers="Anthony Dahanne <anthony.dahanne@softwareag.com>"

# SAG_HOME is defined to /opt/softwareag in the base image
COPY ./legal $SAG_HOME/legal
ADD https://confluence.terracotta.org/download/attachments/29557169/TDB_10.5_terms.pdf?version=1&modificationDate=1570068076106&api=v2 \
 $SAG_HOME/legal/TDB_10.5_terms.pdf

COPY docker/images/terracotta-cluster-tool/entrypoint.sh $SAG_HOME/cluster-tool/
COPY ./tools/cluster-tool/ $SAG_HOME/cluster-tool/
COPY ./tools/common/ $SAG_HOME/common/

# if you have a directory on your docker node that contains a license named license.xml, bind it to this volume !
ENV LICENSES_DIRECTORY "/licenses/"
VOLUME $LICENSES_DIRECTORY
# /configs is an optional volume you can use to deploy configuration files
ENV CONFIGS_DIRECTORY "/configs/"
VOLUME $CONFIGS_DIRECTORY

# This is the current working directory
WORKDIR $SAG_HOME/cluster-tool/

# if you want to fetch the license from an URL, specify the location here
ENV LICENSE_URL ""

# make the JVM aware of the container memory constraints, since JDK 8u131
# see https://blog.csanchez.org/2017/05/31/running-a-jvm-in-a-container-without-getting-killed
ENV JAVA_OPTS "-XX:+UnlockExperimentalVMOptions -XX:+UseCGroupMemoryLimitForHeap"

# the entrypoint needs to be run as root while setting up the config; it run the JVM process as sagadmin though
USER root
# Using exec form to avoid running as subcommand of `/bin/sh -c` which does not pass signals.
# Reference: https://docs.docker.com/engine/reference/builder/#entrypoint
ENTRYPOINT ["./entrypoint.sh"]
