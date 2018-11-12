# Set up Terracotta DB on Minikube

What this document will help you accomplish:
--------------------------------------------

- Prepare a minikube environment locally

- Deploy Terracotta DB on Minikube (with 1 Terracotta Server, 1 sample Ehcache3 client, 1 sample Terracotta Store client, and 1 Terracotta Management Console)

- Deploy Terracotta DB on Minikube (with 2x2 Terracotta Servers - 2 stripes with 2 nodes each, 1 sample Ehcache3 client, 1 sample Terracotta Store client, and 1 Terracotta Management Console)


Prerequisites:
--------------

- Install kubectl (>=1.8) on your laptop (https://kubernetes.io/docs/tasks/tools/install-kubectl/).

- Install minikube (>=0.24.1) on your laptop (https://github.com/kubernetes/minikube/releases)

- Clone this GitHub repository, or, if you prefer, copy paste the deployment files presented in this document

- Have a Terracotta DB license file ready (you can get a trial license file from : http://www.terracotta.org/downloads/ ) - and save it to the previously cloned repo as ```license.xml```


## Getting started with Minikube

Let's start minikube :

    minikube start --cpus 2 --memory 8192

If you plan on using images from an internal registry with self signed certificates, you can use the option : ```--insecure-registry "my.internal.registry.sag:443"```

A nice tool is the dashboard, it's also easy to set up :

    minikube dashboard

Or you can of course use ```kubectl``` :

    kubectl get nodes
    NAME       STATUS    ROLES     AGE       VERSION
    minikube   Ready     <none>    1m        v1.9.0

    kubectl get all
    NAME             TYPE        CLUSTER-IP   EXTERNAL-IP   PORT(S)   AGE
    svc/kubernetes   ClusterIP   10.96.0.1    <none>        443/TCP   2m


## Logging in to Docker Store

Terracotta DB images are distributed via Docker Store.

In a web browser, go to the [TerracottaDB page on Docker Store](https://store.docker.com/images/softwareag-terracottadb)  and then "Proceed to checkout"

![alt text](https://raw.githubusercontent.com/SoftwareAG/terracotta-db-cloud/master/kubernetes/screenshots/terracottadb-on-docker-store.png "Checkout TerracottaDB on Docker Store")

Once this is done, you need to [login to the Docker Store from Minikube](https://kubernetes.io/docs/tasks/configure-pod-container/pull-image-private-registry/#create-a-secret-in-the-cluster-that-holds-your-authorization-token) :

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
          image: store/softwareag/terracotta-server:10.2
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
      Normal   Pulling                14m (x3 over 16m)   kubelet, minikube  pulling image "store/softwareag/terracotta-server:10.2"
      Normal   Pulled                 14m (x3 over 16m)   kubelet, minikube  Successfully pulled image "store/softwareag/terracotta-server:10.2"

You can now delete this pod :

    kubectl delete -f test-pull.yaml
    pod "pull-images" deleted


## Creating the Terracotta cluster Single server, 1 TMC, 1 caching client and 1 store client

First, you need to create the config map for the license :

    kubectl create configmap license --from-file=./license.xml

With the terracotta configuration and the license, you're ready to go :

    kubectl create -f kubernetes/local-minikube/n_clients_1_tc_server_1_tmc.yaml

To open up the TMC page, just issue this command :

    minikube service tmc

### If you're not using the images from Docker Store
In case you have pushed the images to a cloud registry (AWS ECR, GCP Container Registry, etc. ), you'll want to deploy your container from another location than Docker Store.
We suggest you to first export two environment variables, and use sed to replace the image names before sending the deployment file to kubectl

    export IMAGE_PREFIX=my.own.registry:443/terracotta
    export TAG=latest
    sed  -e  "s|store/softwareag/|$IMAGE_PREFIX/|g" -e "s|:10.2|:$TAG|g"  kubernetes/local-minikube/n_clients_1_tc_server_1_tmc/n_clients_1_tc_server_1_tmc.yaml | kubectl create -f -

### Clean things up :

    kubectl delete -f kubernetes/local-minikube/n_clients_1_tc_server_1_tmc.yaml
    minikube ssh
    $(minikube) ls
    backups  dataroots  tmcdata
    $(minikube) sudo rm -rf backups/ dataroots/ tmcdata/


## Appendix A : Minikube addons

You can list, enable or disable addons, such as the dashboard :

    minikube addons list
    minikube addons disable dashboard
    minikube addons enable dashboard

## Appendix B : Reconfigure the cluster

After verifying your cluster is fine (minikube service tmc for example), you can replace the existing tc-confg configmap with the updated configuration file :

    kubectl create configmap tc-config --from-file=kubernetes/local-minikube/reconfigure/tc-config.xml \
        --dry-run -o yaml | kubectl replace -f -

 Now create and deploy your cluster tool reconfigure job :

    kubectl create -f kubernetes/local-minikube/reconfigure/cluster-tool-reconfigure.yml
     job "cluster-tool-reconfigure" created

If necessary, feed kubectl via sed :

    sed  -e  "s|store/softwareag/|$IMAGE_PREFIX/|g" -e "s|:10.2|:$TAG|g"    kubernetes/local-minikube/reconfigure/cluster-tool-reconfigure.yml | kubectl create -f -


Make sure it run properly :

    kubectl logs -f cluster-tool-reconfigure-287bl
    You accepted license agreement
    + su -c 'bin/cluster-tool.sh reconfigure -n MyCluster /configs/tc-config.xml' sagadmin
    License not updated (Reason: Identical to previously installed license)
    Configuration successful

And then, restart your terracotta server :

    kubectl delete pod terracotta-0
    pod "terracotta-0" deleted

Destroying the pod will force Kubernetes to re schedule it, since a statefulset is configured.

If you go back to the TMC, monitoring page, you'll see the offheap increased from 512MB to 920 MB

### Clean things up :

    kubectl delete -f kubernetes/local-minikube/n_clients_1_tc_server_1_tmc.yaml
    kubectl delete -f kubernetes/local-minikube/reconfigure/cluster-tool-reconfigure.yml
    minikube ssh
    $(minikube) ls
    backups  dataroots  tmcdata
    $(minikube) sudo rm -rf backups/ dataroots/ tmcdata/

### Going further

Have a look at [the other examples](../aws-kops/)