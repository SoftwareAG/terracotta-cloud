# Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
# Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG
#
# This Dockerfile creates an image that can run as Terracotta TC Store client

FROM iregistry.eur.ad.sag/ibit/ubi7:jdk8u282-ubi7.9-262_approved
LABEL maintainers="Anthony Dahanne <anthony.dahanne@softwareag.com>"

# SAG_HOME is defined to /opt/softwareag in the base image
COPY ./legal $SAG_HOME/legal
ADD https://confluence.terracotta.org/download/attachments/29557169/TDB_10.5_terms.pdf?version=1&modificationDate=1570068076106&api=v2 \
 $SAG_HOME/legal/TDB_10.5_terms.pdf

COPY client/ $SAG_HOME/client/
COPY docker/images/sample-tcstore-client/src/ $SAG_HOME/
COPY docker/images/sample-tcstore-client/entrypoint.sh $SAG_HOME/

# cluster tool is necessary to check the status of the cluster before starting the client
COPY tools/cluster-tool/ $SAG_HOME/cluster-tool/
COPY tools/common/ $SAG_HOME/common/

# This is the current working directory
WORKDIR $SAG_HOME

# make the JVM aware of the container memory constraints, since JDK 8u131
# see https://blog.csanchez.org/2017/05/31/running-a-jvm-in-a-container-without-getting-killed
ENV JAVA_OPTS "-XX:+UnlockExperimentalVMOptions -XX:+UseCGroupMemoryLimitForHeap"
ENV CLASSPATH ".:/opt/softwareag/client/store/*:/opt/softwareag/client/ehcache/*:/opt/softwareag/client/lib/*:/opt/softwareag/client/logging/*:/opt/softwareag/client/logging/impl/*"

ENV DATAROOT_RESOURCE_NAME "dataroot-2"
ENV OFFHEAP_RESOURCE_NAME "offheap-2"
ENV CLUSTER_NAME "MyCluster"
ENV CUSTOM_TAG "client"
ENV CUSTOM_ALIAS "MyTcStoreClient"
ENV TERRACOTTA_SERVER_URL=terracotta:9410
ENV DATASET_NAME "MyDataset"
# if ENTRY_VALUES_MEAN_SIZE_KB = 128 KB, sizes of values stored in the dataset will be included between 64 and 192 KB
ENV ENTRY_VALUES_MEAN_SIZE_KB "16"
# if NUMBER_OF_RECORDS is set to 3000, the first client will initialize those 3000 records, and all clients will interact with them
ENV NUMBER_OF_RECORDS "10000"
# if POUNDING_INTERVAL_MS is set ot 100, an operation will happen on the client every 100ms
ENV POUNDING_INTERVAL_MS "100"
# if POUNDING_STREAM_INTERVAL is set ot 60, a (costly if not indexed) stream operation will happen on the client every minute
ENV POUNDING_STREAM_INTERVAL "60"
# Set the number of datasets to create and also the number of dataset instances per dataset to create
ENV DATASET_COUNT "1"
ENV INSTANCES_PER_DATASET "2"

RUN javac StoreClientDoingItAll.java

ENTRYPOINT ["./entrypoint.sh"]
