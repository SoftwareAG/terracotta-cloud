# Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
# Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG
#
# This Dockerfile defines an image that can run as Terracotta Server

FROM iregistry.eur.ad.sag/ibit/ubi7:jdk8u282-ubi7.9-262_approved
LABEL maintainers="Abhilash <Abhilash.Abhilash@softwareag.com>"

# SAG_HOME is defined to /opt/softwareag in the base image
COPY ./legal $SAG_HOME/legal
ADD https://confluence.terracotta.org/download/attachments/29557169/TAB_4.3.8_Docker_terms.pdf?version=1&amp;modificationDate=1618838998179&amp;api=v2 \
 $SAG_HOME/legal/TAB_4.3.8_Docker_terms.pdf

COPY docker/images/bigmemorymax-ehcache-client/src/ ./
COPY docker/images/bigmemorymax-ehcache-client/entrypoint.sh ./entrypoint.sh
COPY apis/ ./apis/
COPY code-samples/lib/slf4j-jdk14-1.7.25.jar ./apis/slf4j-jdk14-1.7.25.jar

ENV LICENSE_DIRECTORY "/licenses/"
VOLUME $LICENSE_DIRECTORY

ENV CLASSPATH ".:./apis/ehcache-ee-terracotta-client-all.jar\
:./apis/ehcache/lib/\
:./apis/toolkit/lib/\
:./apis/slf4j-jdk14-1.7.25.jar"

ENV TERRACOTTA_SERVER_URL=terracotta-1:9510,terracotta-2:9510

# make the JVM aware of the container memory constraints, since JDK 8u131
# see https://blog.csanchez.org/2017/05/31/running-a-jvm-in-a-container-without-getting-killed
ENV JAVA_OPTS "-Dcom.tc.productkey.path=$LICENSE_DIRECTORY/license.key -XX:+UnlockExperimentalVMOptions -XX:+UseCGroupMemoryLimitForHeap"

# the entrypoint needs to be run as root while setting up the config; it run the JVM process as sagadmin though
USER root

RUN javac ClientDoingInsertionsAndRetrievals.java

# using the startup script as the entrypoint
ENTRYPOINT ["./entrypoint.sh"]