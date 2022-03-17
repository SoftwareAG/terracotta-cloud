# Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
# Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG
#
# This Dockerfile defines an image that can run as Terracotta Server

FROM iregistry.eur.ad.sag/ibit/ubi7:jdk8u282-ubi7.9-262_approved
LABEL maintainers="Abhilash <Abhilash.Abhilash@softwareag.com>"

# SAG_HOME is defined to /opt/softwareag in the base image
COPY ./legal $SAG_HOME/legal
ADD https://confluence.terracotta.org/download/attachments/29557169/TAB_4.3.10_Docker_terms.pdf?version=1&amp;modificationDate=1618838998179&amp;api=v2 \
 $SAG_HOME/legal/TAB_4.3.10_Docker_terms.pdf

COPY ./server/ $SAG_HOME/server/
COPY docker/images/bigmemorymax-server/config/.tc.custom.logback.xml $SAG_HOME/server/.tc.custom.logback.1.xml
COPY docker/images/bigmemorymax-server/config/logback-jackson-0.1.5.jar $SAG_HOME/server/.
COPY docker/images/bigmemorymax-server/config/logback-json-classic-0.1.5.jar $SAG_HOME/server/.
COPY docker/images/bigmemorymax-server/config/logback-json-core-0.1.5.jar $SAG_HOME/server/.
COPY docker/images/bigmemorymax-server/entrypoint.sh $SAG_HOME/server/entrypoint.sh


# /configs is an optional volume you can use to deploy configuration files
ENV CONFIGS_DIRECTORY "/configs/"
VOLUME $CONFIGS_DIRECTORY

ENV LICENSE_DIRECTORY "/licenses/"
VOLUME $LICENSE_DIRECTORY

ENV JSON_LOGGING "false"
ENV ACCOUNT_ID ""
ENV ENV_NAME ""

# all below commands will now be relative to this path
WORKDIR $SAG_HOME/server

EXPOSE 9510 9530 9540

# default values for offheap, that you can override when starting your container with docker run -e OFFHEAP_RESOURCE1_SIZE=512 for example
ENV OFFHEAP_SIZE "2g"

ENV JAVA_TOOL_OPTIONS "-Dcom.tc.productkey.path=$LICENSE_DIRECTORY/license.key"

# the entrypoint needs to be run as root while setting up the config; it run the JVM process as sagadmin though
USER root

# before starting the terracotta server, we update the tc-config.xml configuration file
ENTRYPOINT ["./entrypoint.sh"]
