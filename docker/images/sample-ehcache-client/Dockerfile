# Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
# Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG
#
# This Dockerfile creates an image that can run as Terracotta Ehcache client

FROM iregistry.eur.ad.sag/ibit/ubi7:jdk8u282-ubi7.9-262_approved
LABEL maintainers="Anthony Dahanne <anthony.dahanne@softwareag.com>"

# SAG_HOME is defined to /opt/softwareag in the base image
COPY ./legal $SAG_HOME/legal
ADD https://confluence.terracotta.org/download/attachments/29557169/TDB_10.5_terms.pdf?version=1&modificationDate=1570068076106&api=v2 \
 $SAG_HOME/legal/TDB_10.5_terms.pdf

COPY client/ $SAG_HOME/client/
COPY docker/images/sample-ehcache-client/src/ $SAG_HOME/
COPY docker/images/sample-ehcache-client/entrypoint.sh $SAG_HOME/
COPY docker/images/sample-ehcache-client/logback.xml $SAG_HOME/

# cluster tool is necessary to check the status of the cluster before starting the client
COPY tools/cluster-tool/ $SAG_HOME/cluster-tool/
COPY tools/common/ $SAG_HOME/common/

# This is the current working directory
WORKDIR $SAG_HOME

# make the JVM aware of the container memory constraints, since JDK 8u131
# see https://blog.csanchez.org/2017/05/31/running-a-jvm-in-a-container-without-getting-killed
ENV JAVA_OPTS "-XX:+UnlockExperimentalVMOptions -XX:+UseCGroupMemoryLimitForHeap"
ENV CLASSPATH ".:/opt/softwareag/client/ehcache/*:/opt/softwareag/client/lib/*:/opt/softwareag/client/logging/*:/opt/softwareag/client/logging/impl/*"

ENV CLUSTER_NAME "MyCluster"
ENV TERRACOTTA_SERVER_URL=terracotta:9410
ENV CACHE_MANAGER_ENTITY_NAME "MyCacheManager"
ENV CUSTOM_TAG "client"
ENV CUSTOM_ALIAS "MyCacheManager"
ENV DATAROOT_RESOURCE_NAME "dataroot-1"
ENV OFFHEAP_RESOURCE_NAME "offheap-1"
ENV RESOURCE_POOL_NAME "my-resource-pool"
ENV RESOURCE_POOL_SIZE "256"
ENV RESOURCE_POOL_SIZE_UNIT "MB"
ENV CLUSTERED_DEDICATED_SIZE "256"
ENV CLUSTERED_DEDICATED_SIZE_UNIT "MB"
# if ENTRY_VALUES_MEAN_SIZE_KB = 128 KB, sizes of values put to the cache will be included between 64 and 192 KB
ENV ENTRY_VALUES_MEAN_SIZE_KB "128"
# if ENTRY_KEYS_SPREAD is set to 1000, the client will do gets and puts on a set of 1000 keys
ENV ENTRY_KEYS_SPREAD "1000"
# if GET_PUT_RATIO is set to 3 -> 3 gets for 1 put - must be an integer greater than 1
ENV GET_PUT_RATIO "3"
# if POUNDING_INTERVAL_MS is set ot 100, a get or put will happen on the client every 100ms
ENV POUNDING_INTERVAL_MS "100"
# By default, a dedicated cache named 'dedicatedcache' is created and 2 shared caches are created: 'shared-cache-1' and 'shared-cache-2'.
# This setting allows to create additional dedicated caches named: 'more-cache-4', 'more-cache-5', etc.
ENV ADDITIONAL_CACHES "0"

RUN javac ClientDoingInsertionsAndRetrievals.java

ENTRYPOINT ["./entrypoint.sh"]
