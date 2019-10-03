#### What is the cluster tool ?

Terracotta Server supports a distributed in-memory data-storage topology, which enables the sharing of data among multiple caches and in-memory

The cluster tool is a command-line utility that allows administrators of the Terracotta Server Array to perform a variety of cluster management tasks. 
For example, the cluster tool can be used to:

- Configure or re-configure a cluster
- Obtain the status of running servers
- Dump the state of running servers
- Stop running servers
- Take backup of running servers


#### How to use this image: QuickStart

0. You can run a terracotta server using (provided you built the terracotta image) :

       docker run -e ACCEPT_EULA=Y -d -p 9410:9410 --name terracotta terracotta-server:10.7.0-SNAPSHOT


1. `Configure` the existing terracotta server running in a docker container with name `terracotta`:
   If the license is available at some URL:
   

        docker run -e ACCEPT_EULA=Y --link terracotta:terracotta -e "LICENSE_URL=http://softwareag.de/license" terracotta-cluster-tool:10.7.0-SNAPSHOT configure -n MyCluster -s terracotta

   Or, if you have your license in a volume with filename `license.xml`:

        docker run -e ACCEPT_EULA=Y --link terracotta:terracotta -v /path/to/my/license/folder:/licenses/ terracotta-cluster-tool:10.7.0-SNAPSHOT configure -n MyCluster -s terracotta

2. Obtain the `status` of running servers:


        docker run -e ACCEPT_EULA=Y --link terracotta:terracotta terracotta-cluster-tool:10.7.0-SNAPSHOT status terracotta
         
   Or, to get the status of the entire cluster:
    
        docker run -e ACCEPT_EULA=Y --link terracotta:terracotta terracotta-cluster-tool:10.7.0-SNAPSHOT status -n MyCluster terracotta
         
3. `Dump` the state of running servers:

        
        docker run -e ACCEPT_EULA=Y --link terracotta:terracotta terracotta-cluster-tool:10.7.0-SNAPSHOT dump terracotta
        
   Or, to dump the entire cluster:
        
        docker run -e ACCEPT_EULA=Y --link terracotta:terracotta terracotta-cluster-tool:10.7.0-SNAPSHOT dump -n MyCluster terracotta
        
4. `Stop` running servers:


        docker run -e ACCEPT_EULA=Y --link terracotta:terracotta terracotta-cluster-tool:10.7.0-SNAPSHOT stop terracotta
        
   Or, to stop the entire cluster:
    
        docker run -e ACCEPT_EULA=Y --link terracotta:terracotta terracotta-cluster-tool:10.7.0-SNAPSHOT stop -n MyCluster terracotta
        
5. Take `backup` of the cluster:

        
        docker run -e ACCEPT_EULA=Y --link terracotta:terracotta terracotta-cluster-tool:10.7.0-SNAPSHOT backup -n MyCluster terracotta


#### How to build this image

To build this Dockerfile

    $ cd terracotta-10.7.0-SNAPSHOT
    $ docker build --file docker/images/cluster-tool/Dockerfile --tag terracotta-cluster-tool:10.7.0-SNAPSHOT .