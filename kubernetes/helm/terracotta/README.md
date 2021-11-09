# Terracotta

The [Terracotta 10.x enterprise](http://www.terracotta.org/) offering includes the following:

 *  Ehcache 3.x compatibility
 *  TC Store compatibility
 *  Distributed In-Memory Data Management with fault-tolerance via Terracotta Server (multi stripes, each with an active and optional mirrors)
 *  In memory off-heap storage - take advantage of all the RAM in your server
 *  Fast Restartable Store - persist to disk in memory data across restarts 


## Quick Start

    helm install --repo=https://softwareag.github.io/terracotta-cloud/ terracotta

## Introduction

This chart bootstraps a [Terracotta cluster](https://hub.docker.com/_/softwareag-terracottadb) deployment on a [Kubernetes](http://kubernetes.io) cluster using the [Helm](https://helm.sh) package manager.

## Prerequisites

- Kubernetes 1.10+
- Helm 2+
- a license file you can get from https://tech.forums.softwareag.com/pub/terracotta-download-form

## Installing the Chart

To install the chart with the release name `my-release`:

```bash
$ helm install --name my-release --repo=https://softwareag.github.io/terracotta-cloud/ terracotta
```

The command deploys Terracotta on the Kubernetes cluster in the default configuration. The [configuration](#configuration) section lists the parameters that can be configured during installation.

> **Tip**: List all releases using `helm list`

## Uninstalling the Chart

To uninstall/delete the `my-release` deployment:

```bash
$ helm delete --purge my-release
```

The command removes all the Kubernetes components associated with the chart and deletes the release.

Specify each parameter using the `--set key=value[,key=value]` argument to `helm install`. For example,

```bash
$ helm install  --repo=https://softwareag.github.io/terracotta-cloud/
  --name=my-cluster \ 
  --namespace=terracotta \ 
  --set tag=10.3.1-SNAPSHOT \
  --set repository=myrepo:443 \
  --set-file licenseFile=~/Downloads/TerracottaDB101.xml    terracotta
```

For a complete list of set options, see [values.yaml](values.yaml)

Alternatively, a YAML file that specifies the values for the parameters can be provided while installing the chart. For example,

```bash
$ helm install --name my-release -f values.yaml terracotta/
```

> **Tip**: You can use the default [values.yaml](values.yaml)
