# Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
# Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG
#
# This Dockerfile defines an image that can run as TMC server

FROM dtr.eur.ad.sag:4443/ibit/ubi7:jdk8u282-ubi7.9-262_approved
LABEL maintainers="Abhilash <Abhilash.Abhilash@softwareag.com>"

# SAG_HOME is defined to /opt/softwareag in the base image
COPY ./legal $SAG_HOME/legal
ADD https://confluence.terracotta.org/download/attachments/29557169/TAB_4.3.9_Docker_terms.pdf?version=1&amp;modificationDate=1618838998179&amp;api=v2 \
 $SAG_HOME/legal/TAB_4.3.9_Docker_terms.pdf

# adding some default configuration files to configure the tmc in non authenticated mode with a connection to tsa:9540 already set-up
COPY docker/images/bigmemorymax-tmc/config/ $SAG_HOME/.tc/mgmt/
COPY docker/images/bigmemorymax-tmc/entrypoint.sh $SAG_HOME/entrypoint.sh
COPY tools/management-console/ $SAG_HOME/

ENV LICENSE_DIRECTORY "/licenses/"
VOLUME $LICENSE_DIRECTORY

# using a custom log4j.properties configuration file to redirect all logs to STDOUT
ENV JAVA_OPTS="-Dcom.tc.productkey.path=$LICENSE_DIRECTORY/license.key -Dlog4j.configuration=$SAG_HOME/.tc/mgmt/log4j.properties"

# the tmc listens on port 9889 and 9443 for browsers connections
EXPOSE 9889 9443

# all below commands will now be relative to this path
WORKDIR $SAG_HOME/

# the entrypoint needs to be run as root while setting up the config; it run the JVM process as sagadmin though
USER root

# using the startup script as the entrypoint
ENTRYPOINT ["./entrypoint.sh"]
