# Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
# Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG
#
# This Dockerfile defines an image including the Terracotta Management Console

FROM iregistry.eur.ad.sag/ibit/ubi7:jdk8u282-ubi7.9-262_approved
LABEL maintainers="Anthony Dahanne <anthony.dahanne@softwareag.com>"

# SAG_HOME is defined to /opt/softwareag in the base image
COPY ./legal $SAG_HOME/legal
ADD https://confluence.terracotta.org/download/attachments/29557169/TDB_10.5_terms.pdf?version=1&modificationDate=1570068076106&api=v2 \
 $SAG_HOME/legal/TDB_10.5_terms.pdf

COPY --chown=1724:1724 tools/management $SAG_HOME/management
COPY docker/images/tmc/entrypoint.sh $SAG_HOME/management/

# the Terracotta Management Server internally uses Terracotta Store datasets to persist statistics and connection information
# let's have this data stored on a volume
ENV TMS_STORAGEFOLDER="/data/tmc/"
VOLUME $TMS_STORAGEFOLDER

# This is the current working directory
WORKDIR $SAG_HOME/management

# the tmc listens on port 9480 for browsers connections
EXPOSE 9480

# the tmc can let the user choose a default cluster url
ENV TMS_DEFAULTURL "terracotta://terracotta:9410"
# we disable logging to file, everything will be logged to stdout and stderr
ENV LOGGING_FILE=''
ENV LOGGING_PATH=''

# make the JVM aware of the container memory constraints, since JDK 8u131
# see https://blog.csanchez.org/2017/05/31/running-a-jvm-in-a-container-without-getting-killed
# for trace logging, you can add : -Dlogging.level.com.terracottatech.management=TRACE
ENV JAVA_OPTS "-XX:+UnlockExperimentalVMOptions -XX:+UseCGroupMemoryLimitForHeap"

# the entrypoint needs to be run as root while setting up the config; it run the JVM process as sagadmin though
USER root

# using the startup script as the entrypoint
ENTRYPOINT ["./entrypoint.sh"]
