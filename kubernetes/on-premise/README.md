# Set up Terracotta DB on premise, using Kubernetes

What this document will help you accomplish:
--------------------------------------------

- Deploy Terracotta DB on a Kubernetes cluster, running on premise (we won'tcover here how to install Kubernetes on premise) with the following topology : 2x2 Terracotta Servers - 2 stripes with 2 nodes each, n sample Ehcache3 clients, n sample Terracotta Store clients, and 1 Terracotta Management Console)


Prerequisites:
--------------

- Have a Kubernetes (>= 1.8) cluster properly configured on your premise, with a few worker nodes available

- Install kubectl (>=1.8) on your laptop (https://kubernetes.io/docs/tasks/tools/install-kubectl/).

- Clone this GitHub repository, or, if you prefer, copy paste the deployment files presented in this document

- Have a Terracotta DB license file ready (you can get a trial license file from : http://www.terracotta.org/downloads/ ) - and save it to the previously cloned repo as ```license.xml```


## Getting Started

In case you connect to your cluster using a generated configuration file (admin.conf for example) you would need to feed all your kubectl commands with :
    
    kubectl --kubeconfig=admin.conf
    

Let's verify your cluster looks healthy, the following command will give you the nodes of the kubernetes cluster:

    kubectl get nodes

A nice tool is the dashboard, it's easy to set that up :

    kubectl create -f https://raw.githubusercontent.com/kubernetes/kops/master/addons/kubernetes-dashboard/v1.8.1.yaml

To access it

    kubectl proxy

And then open this url : 
    
    http://localhost:8001/ui

## Logging in to Docker Store

Terracotta DB images are distributed via Docker Store.
After logging in to [Docker Store](https://store.docker.com/) (if you already have a DockerHub account, you can use the same credentials), go and "Proceed to Checkout" for the 5 following Docker images :

- [Terracotta Server](https://store.docker.com/images/softwareag-terracotta-server)
- [Terracotta Management Console](https://store.docker.com/images/softwareag-tmc)
- [Cluster Tool](https://store.docker.com/images/softwareag-terracotta-cluster-tool)
- [Sample Ehcache Client](https://store.docker.com/images/softwareag-sample-ehcache-client)
- [Sample Terracotta Store Client](https://store.docker.com/images/softwareag-sample-tcstore-client)

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
          image: store/softwareag/terracotta-server-oss:5.3
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
      Normal   Pulling                14m (x2 over 16m)   kubelet, minikube  pulling image "store/softwareag/tmc:10.2"
      Normal   Pulled                 14m (x2 over 14m)   kubelet, minikube  Successfully pulled image "store/softwareag/tmc:10.2"
      Normal   Pulling                14m (x3 over 16m)   kubelet, minikube  pulling image "store/softwareag/terracotta-server:10.2"
      Normal   Pulled                 14m (x3 over 16m)   kubelet, minikube  Successfully pulled image "store/softwareag/terracotta-server:10.2"

You can now delete this pod :

    kubectl delete -f test-pull.yaml
    pod "pull-images" deleted

### If you're not using the images from Docker Store
In case you have pushed the images to a cloud registry (AWS ECR, GCP Container Registry, etc. ), you'll want to pull your images from another location than Docker Store.
We suggest you to first export two environment variables, and use sed to replace the image names before sending the deployment file to kubectl

    export IMAGE_PREFIX=my.own.registry:443/terracotta
    export TAG=latest
    sed  -e  "s|store/softwareag/|$IMAGE_PREFIX/|g" -e "s|:10.2|:$TAG|g"  ./kubernetes/on-premise/n_clients_4_tc_server_1_tmc.yaml | kubectl apply -f -


## Creating a Terracotta cluster

First, you need to create the config maps for the config files and the license, so that we can pass these to the terracotta containers.:

    kubectl create configmap tc-configs --from-file=./kubernetes/on-premise/stripe1.xml --from-file=./kubernetes/on-premise/stripe2.xml
    kubectl create configmap license --from-file=./license.xml

We'll now assume that you want to bind volumes to the worker nodes filesystem (if you'd rather use NFS, see next section)
You would want to use the worker nodes local filesystem to leverage a very fast local drive (SSD NVMe for example)

Have a look at the file named ```kubernetes/on-premise/persistent_volumes_and_claims-local.yaml``` , and feel free to change the path on the worker nodes.

Now run the following command to proivision the Persistent Volume and Persistent Volume Claims :

    kubectl create -f ./kubernetes/on-premise/persistent_volumes_and_claims-local.yaml

And then, deploy the cluster :

     kubectl apply -f ./kubernetes/on-premise/n_clients_4_tc_server_1_tmc.yaml


### Destroying the Terracotta cluster

    kubectl delete -f ./kubernetes/on-premise/n_clients_4_tc_server_1_tmc.yaml

## Appendix A : Using NFS volumes for storage

First, make sure you have created the license and tc-configs ConfigMaps.

Then, have a look at the file named : ```kubernetes/on-premise/persistent_volumes_and_claims-nfs.yaml``` , and feel free to change the path and NFS server coordinates; also make sure the NFS shares already exist !

Now run the following command to proivision the Persistent Volume and Persistent Volume Claims :

    kubectl create -f ./kubernetes/on-premise/persistent_volumes_and_claims-nfs.yaml

Before deploying the cluster, replace the ```-local``` with ```-nfs``` in the file named ```./kubernetes/on-premise/n_clients_4_tc_server_1_tmc.yaml```

And then, deploy the cluster :

     kubectl apply -f ./kubernetes/on-premise/n_clients_4_tc_server_1_tmc.yaml


## Appendix B : binding containers to hosts

If you chose to use local (to the worker nodes) Persistent Volumes, you may want to make sure the pods "stick" to the same worker nodes.
In that case, you can use affinity to require pods to run exclusively on a given worker node.

      affinity:
        nodeAffinity:
          requiredDuringSchedulingIgnoredDuringExecution:
            nodeSelectorTerms:
            - matchExpressions:
              - key: "kubernetes.io/hostname"
                operator: In
                values:
                - k8s-worker-node-002.sag