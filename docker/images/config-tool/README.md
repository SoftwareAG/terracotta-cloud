#### What is the cluster tool ?

Terracotta Server supports a distributed in-memory data-storage topology, which enables the sharing of data among multiple caches and in-memory

Config tool provides a suite of commands used for managing the cluster topology and configuration, among other things.

#### How to use this image: QuickStart

0. You can run a terracotta server using (provided you built the terracotta image) :

       docker run -e ACCEPT_EULA=Y --name terracotta --hostname terracotta -p 9410:9410 -d terracotta-server:10.11

1. The `activate` command makes a cluster ready to be used by Terracotta clients.
    If the license is available at some URL:
        docker run -e ACCEPT_EULA=Y --link terracotta:terracotta -e "LICENSE_URL=http://softwareag.de/license" terracotta-config-tool:10.11 activate -l /licenses/license.xml -n tc-cluster -s terracotta

    Or, if you have your license in a volume with filename `license.xml`:

        docker run -e ACCEPT_EULA=Y --link terracotta:terracotta -v /path/to/my/license/folder:/licenses/ terracotta-config-tool:10.11 activate -l /licenses/license.xml -n tc-cluster -s terracotta

2. The `attach` command builds a cluster by constructing stripes from nodes, and cluster from stripes respectively.

3. The `detach` command allows removal of nodes from stripes, and stripes from cluster respectively.

4. The `set` & `unset` command updates configuration settings on individual nodes, stripes or the entire cluster, depending on the specified namespace.

#### How to build this image

To build this Dockerfile

    $ cd terracotta-10.11.0-SNAPSHOT
    $ docker build --file docker/images/config-tool/Dockerfile --tag terracotta-config-tool:10.11.0-SNAPSHOT .