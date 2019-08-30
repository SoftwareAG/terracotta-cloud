# Docker Images definitions for Terracotta 10 Enterprise

## Goal

The Terracotta Docker images for Terracotta 10 are distributed officially via [Docker Cloud](https://hub.docker.com/_/softwareag-terracottadb).

If you're curious how those images are built, read on! Those Dockerfiles are the ones we use to build the Terracotta 10 images.

## Requirements to build

* You'll need to locally have a "native" kit, unzipped - [you can get one from Terracotta.org](http://www.terracotta.org/downloads/)
* A Terracotta license  ([you can start using trial ones](https://www.terracotta.org/retriever.php?n=TerracottaDB101linux.xml ))
* A base image with a Java JVM. (the ones we used are usually based on [CentOS](https://hub.docker.com/_/centos) or [UBI](https://www.redhat.com/en/blog/introducing-red-hat-universal-base-image) with a [Zulu JVM](https://www.azul.com/downloads/zulu-community/))

Replacing our base image would look like:

```dockerfile
#FROM daerepository03.eur.ad.sag:4443/ibit/ubi7:ubi-7-jdk-test
FROM centos:7
ENV SAG_HOME=/opt/softwareag     
RUN groupadd -g 1724 sagadmin && useradd -u 1724 -m -g 1724 -d ${SAG_HOME} -c "SoftwareAG Admin" sagadmin && mkdir -p ${SAG_HOME} && chown 1724:1724 ${SAG_HOME} && chmod 775 ${SAG_HOME}
USER 1724              
ADD --chown=1724:1724 jvmfolder/ /opt/softwareag           
ENV JAVA_HOME=/opt/softwareag/jvm/jvm
ENV PATH=/opt/softwareag/jvm/jvm/bin:/opt/softwareag:/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin               
```


## How to build

All the instructions are present in the sub folders !

That said, here are some command lines worth of mention.

In an unzipped Terracotta kit, 

* Export the version, either 10.3 or 10.5

      export VERSION=10.3

* fetch the docker folder from this repo

      curl -L https://github.com/SoftwareAG/terracotta-cloud/archive/master.tar.gz | tar xvz &&  mv terracotta-cloud-master/docker/$VERSION docker
    
* Build all the images

```
docker build --file docker/images/cluster-tool/Dockerfile --tag terracotta-cluster-tool:$VERSION .
docker build --file docker/images/sample-ehcache-client/Dockerfile --tag sample-ehcache-client:$VERSION .
docker build --file docker/images/sample-tcstore-client/Dockerfile --tag sample-tcstore-client:$VERSION .
docker build --file docker/images/server/Dockerfile --tag terracotta-server:$VERSION .
docker build --file docker/images/tmc/Dockerfile --tag tmc:$VERSION .
```
For 10.3 only:

    docker build --file docker/images/websessions-cart-example/Dockerfile --tag websessions-cart-example:$VERSION .


* Run the smoke tests
    
      cd docker/smoke-test
      ./smoke-test.sh
    
