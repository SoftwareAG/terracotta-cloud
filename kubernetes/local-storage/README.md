# Set up Terracotta DB on a Kubernetes cluster (set up with on premise)

What this document will help you accomplish:
--------------------------------------------

- Set up a Kubernetes cluster on premise, using local storage

- Deploy Terracotta DB on a Kubernetes cluster (with 2x2 Terracotta Servers - 2 stripes with 2 nodes each, n sample Ehcache3 clients, n sample Terracotta Store clients, and 1 Terracotta Management Console)

- Scale out scenario : start with 2 worker nodes, scale to 4 and increase offheap memory

Prerequisites:
--------------

- Install kubectl (>=1.9) on your laptop (https://kubernetes.io/docs/tasks/tools/install-kubectl/).

- Clone this GitHub repository, or, if you prefer, copy paste the deployment files presented in this document

- Have a Terracotta DB license file ready (you can get a trial license file from : http://www.terracotta.org/downloads/ ) - and save it to the previously cloned repo as ```license.xml```


## Getting started

We assume here that you have a Kubernetes cluster handy with at least 5 worker nodes with enough memory to handle the offheap the Terracotta server will use.

We also assume you have control of the worker nodes with ssh access

Finally, kubectl should be configured to connect to this Kubernetes cluster


## Logging in to Docker Store

Terracotta DB images are distributed via Docker Store; but if the images are stored on another registry, you can skip that part.

In a web browser, go to the [TerracottaDB page on Docker Store](https://store.docker.com/images/softwareag-terracottadb)  and then "Proceed to checkout"

![alt text](https://raw.githubusercontent.com/SoftwareAG/terracotta-db-cloud/master/kubernetes/screenshots/terracottadb-on-docker-store.png "Checkout TerracottaDB on Docker Store")

Once this is done, you need to pass your credentials to kubectl, and store them as a secret :

    kubectl create secret docker-registry regcred --docker-server=https://index.docker.io/v1/ --docker-username=<your-name> --docker-password=<your-pword> --docker-email=<your-email>

To make sure minikube can properly pull from Docker Store, you can try and apply this sample yaml file :

    cat > test-pull.yaml <<EOF
    apiVersion: v1
    kind: Pod
    metadata:
      name: pull-images
    spec:
      containers:
        - name: test-terracotta-server-pull
          image: store/softwareag/terracotta-server:10.3
          imagePullPolicy: Always
          command: [ "echo", "SUCCESS" ]
      imagePullSecrets:
       - name: regcred
    EOF

After executing the previous cat command, you can deploy test-pull.yaml :

    kubectl create -f test-pull.yaml

To make sure everything went fine, give it some time and read the describe output :

    kubectl describe -f test-pull.yaml
    Events:
      Type     Reason                 Age                 From               Message
      ----     ------                 ----                ----               -------
      Normal   Scheduled              16m                 default-scheduler  Successfully assigned pull-images to minikube
      Normal   Pulling                14m (x3 over 16m)   kubelet, minikube  pulling image "store/softwareag/terracotta-server:10.3"
      Normal   Pulled                 14m (x3 over 16m)   kubelet, minikube  Successfully pulled image "store/softwareag/terracotta-server:10.3"

You can now delete this pod :

    kubectl delete -f test-pull.yaml
    pod "pull-images" deleted

## If you're not using the images from Docker Store
In case you have pushed the images to a cloud registry (AWS ECR, GCP Container Registry, etc. ), you'll want to pull your images from another location than Docker Store.
We suggest you to first export two environment variables, and use sed to replace the image names before sending the deployment file to kubectl

    export IMAGE_PREFIX=my.own.registry:443/terracotta
    export TAG=latest
    sed  -e  "s|store/softwareag/|$IMAGE_PREFIX/|g" -e "s|:10.3|:$TAG|g"  kubernetes/aws-kops/n_clients_4_tc_server_1_tmc.yaml | kubectl apply -f -



## Prepare your worker nodes

TerracottaDB has strong requirements in terms of file system : we recommend our users to rely on fast local storage (NVMe Solid State Drives for example)
Working with local filesystem on Kubernetes is possible, but involves few steps; let's go through them :

* decide which worker nodes are going to run the Terracotta Servers and the TMC, and assign labels

In the case you have 4 workers nodes dedicated to the Terracotta cluster (1 Terracotta Server pod max. will run on each), and 1 worker node for the TMC :

    kubectl label nodes tc-k8s-001 terracotta-a=
    kubectl label nodes tc-k8s-002 terracotta-b=
    kubectl label nodes tc-k8s-003 terracotta-c=
    kubectl label nodes tc-k8s-004 terracotta-d=
    kubectl label nodes tc-k8s-005 terracotta=tmc

Let's verify it looks like this :

    kubectl get nodes -o=jsonpath='{range .items[*]}{.metadata.name}{"\t"}{.metadata.labels}{"\n"}{end}'
    tc-k8s-001   terracotta-a:
    tc-k8s-002   terracotta-b:
    tc-k8s-003   terracotta-c:
    tc-k8s-004   terracotta-d:
    tc-k8s-005   tmc
    tc-k8s-006   
    tc-k8s-master-001    

On the 4 servers nodes (tc-k8s-001, tc-k8s-002, tc-k8s-003, tc-k8s-004), let's create a folder that will host our Terracotta FRS persistence :

    ssh anthony@tc-k8s-001
    mkdir /data/tcdata-A
    
    ssh anthony@tc-k8s-002
    mkdir /data/tcdata-B

    ssh anthony@tc-k8s-003
    mkdir /data/tcdata-C
      
    ssh anthony@tc-k8s-004
    mkdir /data/tcdata-D
 
    
On the TMC worker node :

    ssh anthony@tc-k8s-005
    mkdir /data/tmcdata

Now, let's tell Kubernetes to use local disks as the default storage class

    kubectl apply -f kubernetes/aws-kops/local-storage/storageclass.yaml
    storageclass.storage.k8s.io/fast-disks configured
    
And let's create the persistent volumes that will be used during deployment :

    kubectl apply -f kubernetes/aws-kops/local-storage/tc-pv.yaml
    kubectl apply -f kubernetes/aws-kops/local-storage/tmc-pv.yaml
            

## Creating the Terracotta cluster

First, you need to create the configmap for the license :

    kubectl create configmap license --from-file=./license.xml

And then, you need to submit the configuration for the Terracotta servers :

    kubectl apply -f kubernetes/aws-kops/local-storage/tc-servers-configmap.yaml

You should be all set configuration wise :

    kubectl get configmaps
    NAME         DATA   AGE
    license      1      3d4h
    tc-configs   2      3d3h
    
    

You're now ready to go :

    kubectl apply -f kubernetes/aws-kops/local-storage/tc-servers-deployment-and-service-on-premise.yaml
    
After few minutes, your Terracotta servers pods should all be running : 


    $ kubectl get all                                                                                                                                                                                                      MCADAH02.local: Tue Feb 26 17:12:42 2019
    
    NAME                     READY   STATUS      RESTARTS   AGE
    pod/terracotta-1-0       2/2     Running     1          8m49s
    pod/terracotta-1-1       2/2     Running     1          6m12s
    pod/terracotta-2-0       2/2     Running     3          14m
    pod/terracotta-2-1       2/2     Running     1          12m

It's time to configure the cluster, using the cluster tool, and to deploy the Terracotta Management Console webapp :
    
    kubectl apply -f kubernetes/aws-kops/local-storage/cluster-tool-configure-job.yaml
    kubectl apply -f kubernetes/aws-kops/local-storage/tmc-deployment-and-service-on-premise.yaml

Now head to the dashboard, look for services and click on the TMC link; you're about to monitor your cluster !

It's now time to deploy your Ehcache and TCStore workloads; they will be able to connect to the Terracotta cluster using this URI : terracotta-1-0.stripe-1:9410,terracotta-1-1.stripe-1:9410,terracotta-2-0.stripe-2:9410,terracotta-2-1.stripe-2:9410
We have conveniently provided some sample client applications for you to try things out:

    kubectl apply -f kubernetes/aws-kops/local-storage/caching-client-deployment.yaml
    kubectl apply -f kubernetes/aws-kops/local-storage/store-client-deployment.yaml
            

## Moving a Terracotta server to another worker node

* scale down the stripe (for example terracotta-1 stateful set) to 1.
* Identify which PVC terracotta-1-1 was using 

    kubectl get pvc
    tcdata-terracotta-1-1   Bound    tcdata-a   50Gi       RWO            fast-disks     19h
    
* ssh to the node that provided the volume (for example tc-k8s-001)
* copy its persisted data to the new node (for example tc-k8s-005)
    
    scp -r /data/tcdata-a/ anthony@tc-k8s-005:/data/tcdata-a

* unlabel tc-k8s-001   
    
    kubectl label nodes tc-k8s-001 terracotta-a-

* label tc-k8s-005

    kubectl label nodes tc-k8s-005 terracotta-a=
    
* scale up the stripe to 2
* done !

## Starting with 2 worker nodes and spreading to 4 workers nodes during scale up

With TerracottaDB, you can safely run 2 stripes with 1 Active, 1 Passive each using only 2 worker nodes (making sure only 1 server of each stripe runs on the same node)

In case you need more memory after an offheap reconfiguration, you can re distribute your tc server nodes on 2 additional worker nodes.

Let's go through all the necessary steps to perform such a scale operation.

Let's starting labelling the nodes for local storage

    kubectl label nodes tc-k8s-001 terracotta-a=
    kubectl label nodes tc-k8s-001 terracotta-b=
    kubectl label nodes tc-k8s-002 terracotta-c=
    kubectl label nodes tc-k8s-003 terracotta-d=
    kubectl label nodes tc-k8s-005 terracotta=tmc

Let's verify it looks like this :

    kubectl get nodes -o=jsonpath='{range .items[*]}{.metadata.name}{"\t"}{.metadata.labels}{"\n"}{end}'
    tc-k8s-001   terracotta-a: terracotta-b:
    tc-k8s-002   terracotta-c: terracotta-d:
    tc-k8s-005   tmc


On the 2 servers nodes (tc-k8s-001, tc-k8s-002), let's create a folder that will host our Terracotta FRS persistence :

    ssh anthony@tc-k8s-001
    mkdir /data/tcdata-a
    mkdir /data/tcdata-b
    
    ssh anthony@tc-k8s-002
    mkdir /data/tcdata-c
    mkdir /data/tcdata-d
 
    
On the TMC worker node :

    ssh anthony@tc-k8s-005
    mkdir /data/tmcdata

Now, you can follow the regular instructions to deploy the workloads; make sure though to use scale-up/tc-servers-deployment-and-service-2-for-each-node.yaml to deploy the servers

###Time to scale out

If you want to increase offheap size, and current worker nodes can't provide enough memory, then you can reconfigure your tc servers before relocating them to their own nodes.

First, you need to reconfigure the offheap sizes to the target size; let's imagine we want to change offheap-1 from 8 GB to 16 GB

    $ kubectl apply -f kubernetes/local-storage/scale-up/tc-servers-configmap.yaml
    $ kubectl apply -f kubernetes/local-storage/scale-up/cluster-tool-reconfigure-job.yaml

Then, you need to scale down each stripe to 1 member only (that will let only 1 Active in each stripe running)

    $ kubectl scale statefulset --replicas=1 terracotta-1
    statefulset.apps/terracotta-1 scaled
    $ kubectl scale statefulset --replicas=1 terracotta-2
    statefulset.apps/terracotta-1 scaled


It's time to copy the data of the 2 lost passives into the new nodes; in my example situation, terracotta-1-1 (attached to volume tcdata-c) and terracotta-2-1 (attached to volume tcdata-b) were brought down

    $ kubectl get pvc
    NAME                    STATUS   VOLUME     CAPACITY   ACCESS MODES   STORAGECLASS   AGE
    tcdata-terracotta-1-0   Bound    tcdata-a   50Gi       RWO            fast-disks     16h
    tcdata-terracotta-1-1   Bound    tcdata-c   50Gi       RWO            fast-disks     16h
    tcdata-terracotta-2-0   Bound    tcdata-d   50Gi       RWO            fast-disks     16h
    tcdata-terracotta-2-1   Bound    tcdata-b   50Gi       RWO            fast-disks     16h
    tmcdata-tmc-0           Bound    tmcdata    20Gi       RWO            fast-disks     15h
    
we want to reschedule terracotta-1-1 and terracotta-2-1 to the new nodes tc-k8s-003 and tc-k8s-004.

Let's move the needed storage to the new nodes :

     $ ssh anthony@tc-k8s-002
     $ scp -r /data/tcdata-c/ anthony@tc-k8s-004:/data/tcdata-c
     $ ssh anthony@tc-k8s-001  
     $ scp -r /data/tcdata-b/ anthony@tc-k8s-003:/data/tcdata-b

Then, you'll need to make sure you have 2 new worker nodes available, and then you'll need to update the labels, to have something similar to this :

    kubectl label nodes tc-k8s-004 terracotta-c=
    kubectl label nodes tc-k8s-003 terracotta-b=
    kubectl label nodes tc-k8s-001 terracotta-b-
    kubectl label nodes tc-k8s-002 terracotta-c-

Hint : to remove a label, you can simply use (pay attention to the "-" and not "=") :

    kubectl label nodes tc-k8s-001 terracotta-b-


Apply the following manifest to have the passives restarted on their new nodes; they should reload from disk the previous data we just copied.

    kubectl apply -f kubernetes/local-storage/scale-up/tc-servers-deployment-and-service-2-for-each-node.yaml

Final step : restarting the servers that never got restarted since the reconfiguration; locate them (they should be the active servers) and kill their pods :

    kubectl delete pod terracotta-1-0
    kubectl delete pod terracotta-2-0
            
Kubernetes will automatically reschedule them on their previous nodes.


## Destroying the Terracotta cluster
    
    kubectl delete statefulsets.apps terracotta-1 terracotta-2 tmc
    kubectl delete pvc tcdata-terracotta-1-0  tcdata-terracotta-1-1  tcdata-terracotta-2-0  tcdata-terracotta-2-1  tmcdata-tmc-0
    kubectl delete pv tcdata-1-1  tcdata-1-2  tcdata-2-1  tcdata-2-2  tmcdata
    kubectl delete service terracotta-1 terracotta-2 tmc

And also :
    
    kubectl delete jobs cluster-tool
    kubectl delete deployments caching-client
    kubectl delete deployments store-client
