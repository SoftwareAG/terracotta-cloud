# Set up Terracota DB on AWS, using Kops

What this document will help you accomplish:
--------------------------------------------

- Set up a Kubernetes cluster on AWS, using kops

- Deploy Terracotta DB on a Kubernetes cluster, running on AWS, using kops (with 2x2 Terracotta Servers - 2 stripes with 2 nodes each, n sample Ehcache3 clients, n sample Terracotta Store clients, and 1 Terracotta Management Console)


Prerequisites:
--------------

- Install kubectl (>=1.8) on your laptop (https://kubernetes.io/docs/tasks/tools/install-kubectl/).

- Install kops (>=1.8.1) on your laptop (https://github.com/kubernetes/kops)

- Optionally, you can install the AWS cli (for advanced set up, such as EFS volume creation) (https://aws.amazon.com/cli/)

- Clone this GitHub repository, or, if you prefer, copy paste the deployment files presented in this document

- Have a Terracotta DB license file ready (you can get a trial license file from : http://www.terracotta.org/downloads/ ) - and save it to the previously cloned repo as ```license.xml```


## Getting started : set up a Kubernetes cluster on AWS, with kops

We recommend you to first read the up to date kops documentation  : https://github.com/kubernetes/kops/blob/master/docs/aws.md

After you have created an AWS user with its key, password, and gave it the necessary rights, you can create a s3 store to hold your kops configuration, and get started :

    export KOPS_STATE_STORE=s3://terracotta-kops
    export NAME=tc-cluster.k8s.local
    #replace ca-central-1a with the closest AWS location
    export AWS_ZONE=ca-central-1a
    # create the cluster configuration, with default master and 4 large nodes, for the tc servers
    kops create cluster --zones ${AWS_ZONE} ${NAME} --node-count=4 --node-size=m4.2xlarge
    # create an additionnal instance group, a group of nodes for the clients
    kops create ig --name=${NAME} --subnet ${AWS_ZONE}  clientnodes --edit
    # you'll be prompted to provide the clientnodes group specification, use this :
    #  machineType: m4.xlarge
    #  maxSize: 6
    #  minSize: 6
    # submit the validation and lets kops create the k8s cluster
    kops update cluster $NAME --yes

After few minutes, commands such as :

    kops validate cluster

should return :

    Using cluster from kubectl context: tc-cluster.k8s.local

    Validating cluster tc-cluster.k8s.local

    INSTANCE GROUPS
    NAME                    ROLE    MACHINETYPE     MIN     MAX     SUBNETS
    clientnodes             Node    m4.xlarge       6       6       ca-central-1a
    master-ca-central-1a    Master  c4.large        1       1       ca-central-1a
    nodes                   Node    m4.2xlarge      4       4       ca-central-1a

    NODE STATUS
    NAME                                            ROLE    READY
    ip-172-20-32-66.ca-central-1.compute.internal   node    True
    ip-172-20-35-105.ca-central-1.compute.internal  node    True
    ip-172-20-35-249.ca-central-1.compute.internal  master  True
    ip-172-20-38-18.ca-central-1.compute.internal   node    True
    ip-172-20-39-146.ca-central-1.compute.internal  node    True
    ip-172-20-44-133.ca-central-1.compute.internal  node    True
    ip-172-20-46-124.ca-central-1.compute.internal  node    True
    ip-172-20-48-24.ca-central-1.compute.internal   node    True
    ip-172-20-51-76.ca-central-1.compute.internal   node    True
    ip-172-20-56-247.ca-central-1.compute.internal  node    True
    ip-172-20-63-226.ca-central-1.compute.internal  node    True

and also

    kubectl get node

should return :

    NAME                                             STATUS    ROLES     AGE       VERSION
    ip-172-20-32-66.ca-central-1.compute.internal    Ready     node      3m        v1.8.7
    ip-172-20-35-105.ca-central-1.compute.internal   Ready     node      3m        v1.8.7
    ip-172-20-35-249.ca-central-1.compute.internal   Ready     master    4m        v1.8.7
    ip-172-20-38-18.ca-central-1.compute.internal    Ready     node      3m        v1.8.7
    ip-172-20-39-146.ca-central-1.compute.internal   Ready     node      3m        v1.8.7
    ip-172-20-44-133.ca-central-1.compute.internal   Ready     node      3m        v1.8.7
    ip-172-20-46-124.ca-central-1.compute.internal   Ready     node      3m        v1.8.7
    ip-172-20-48-24.ca-central-1.compute.internal    Ready     node      3m        v1.8.7
    ip-172-20-51-76.ca-central-1.compute.internal    Ready     node      3m        v1.8.7
    ip-172-20-56-247.ca-central-1.compute.internal   Ready     node      3m        v1.8.7
    ip-172-20-63-226.ca-central-1.compute.internal   Ready     node      3m        v1.8.7


### Kubernetes dashboard
A nice tool is the dashboard, it's easy to set up :

    kubectl create -f https://raw.githubusercontent.com/kubernetes/kops/master/addons/kubernetes-dashboard/v1.8.1.yaml
    kubectl proxy

And then open this url : http://localhost:8001/ui

### DANGER ZONE !
Once you're done with your cluster, don't forget to delete all AWS resources :

    kops delete cluster --name $NAME --yes

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
    sed  -e  "s|store/softwareag/|$IMAGE_PREFIX/|g" -e "s|:10.2|:$TAG|g"  kubernetes/aws-kops/n_clients_4_tc_server_1_tmc.yaml | kubectl apply -f -


## Creating the Terracotta cluster

First, you need to create the config maps for the configuration files and the license :

    kubectl create configmap tc-configs --from-file=./kubernetes/aws-kops/stripe1.xml --from-file=./kubernetes/aws-kops/stripe2.xml
    kubectl create configmap license --from-file=./license.xml

Also, you need storage for your dataroots - we'll use EBS - and since EBS is the kops default storage, you just need to create the Persistent Volume Claims.
Each claim will create an EBS volume automatically, since, by default kops uses EBS as a back end for the claims.

    kubectl create -f kubernetes/aws-kops/dataroots-persistent-volumes-claims.yaml

That said, if you're using and EFS volume for backups (see appendix), don't forget to create a persistent volume for it :

    kubectl create -f kubernetes/aws-kops/persistent-volumes.yaml

With configs, license and storage, you're ready to go :

    kubectl apply -f kubernetes/aws-kops/n_clients_4_tc_server_1_tmc.yaml

Now head to the dashboard, look for Services and click on the TMC link; you're about to monitor your cluster !


## Appendix A : pod assignation policies

A deprecated option, but still valid, is using the nodeSelector

    kubectl label nodes ip-172-20-48-202.ca-central-1.compute.internal tc-server=true
    kubectl label nodes ip-172-20-60-84.ca-central-1.compute.internal client=true
    kubectl get nodes --show-labels

And then you tell your containers to be deployed only to such nodes with :

      nodeSelector:
        client: "true"

But that's not super flexible.
A better way is using affinity and anti affinity (https://kubernetes.io/docs/concepts/configuration/assign-pod-node/)


      affinity:
        podAntiAffinity:
          requiredDuringSchedulingIgnoredDuringExecution:
          - labelSelector:
              matchExpressions:
              - key: app-role
                operator: In
                values:
                - terracotta-server
            topologyKey: kubernetes.io/hostname
        nodeAffinity:
          requiredDuringSchedulingIgnoredDuringExecution:
            nodeSelectorTerms:
            - matchExpressions:
              - key: beta.kubernetes.io/instance-type
                operator: In
                values:
                - m5.12xlarge


That reads : only assign the container to a node that is m5.12xlarge and not already running a pod labeled with app-role: terracotta-server

## Appendix B EBS not default Volumes

Create your own Persistent Volume (not needed by default since with kops, they'll be created when requesting the PVC)

    aws ec2 create-volume \
        --region ca-central-1 \
        --availability-zone ca-central-1a \
        --size 5 \
        --volume-type gp2

## Appendix C EFS backed Volumes

EFS, a glorified NFS v4.1, can be an interesting option for backups (since it does not have to be bound to a single instance) - beware it's only available in few regions.

First things first, make sure your AWS kops user has the required rights to create an EFS file system : it should have AmazonElasticFileSystemFullAccess

To create an EFS filesystem, just issue this command :

    aws --region=us-east-1 efs create-file-system --creation-token random-token
    {
        "OwnerId": "67XXXXX",
        "CreationToken": "random-token",
        "FileSystemId": "fs-XXXXXXX",
        "CreationTime": 1513400669.0,
        "LifeCycleState": "creating",
        "NumberOfMountTargets": 0,
        "SizeInBytes": {
            "Value": 0
        },
        "PerformanceMode": "generalPurpose",
        "Encrypted": false
    }

Then, make sure it's going to be available to your kops cluster :

    aws --region=us-east-1 efs create-mount-target --file-system-id=fs-XXXXXXX --subnet-id=subnet-daXXXX --security-groups=sg-ceXXXX
    {
        "OwnerId": "67XXXXX",
        "MountTargetId": "fsmt-XXXXX",
        "FileSystemId": "fs-XXXXXXX",
        "SubnetId": "subnet-daXXXX",
        "LifeCycleState": "creating",
        "IpAddress": "172.20.53.35",
        "NetworkInterfaceId": "eni-XXXX"
    }

You can find out your security group looking for the instance group id (nodes here) :

    aws --region=us-east-1 ec2 describe-security-groups | grep -A 15 nodes | grep GroupId
                                "GroupId": "sg-ceXXXXX",

You can find out your subnet listing your instances :

    aws --region=us-east-1 ec2 describe-instances | grep -A 30 nodes.tc-cluster15c.k8s.local | grep SubnetId
                                "SubnetId": "subnet-daXXXX",

Make sure you're mounting the file system properly using a persistent volume and a persistent volume claim :


    apiVersion: v1
    kind: PersistentVolume
    metadata:
      name: volume-name
    spec:
      capacity:
        storage: 10Gi
      accessModes:
       - ReadWriteMany
      nfs:
        server: fs-XXXXXXX.efs.us-east-1.amazonaws.com
        path: "/"
    ---

    apiVersion: v1
    kind: PersistentVolumeClaim
    metadata:
      name: volume-claim-name
      labels:
        app: myapp
    spec:
      accessModes:
       - ReadWriteMany
      resources:
        requests:
          storage: 10Gi
      volumeName: volume-name
      storageClassName: ""

    ---

    apiVersion: v1
    kind: Pod
    metadata:
      name: nfs-web
    spec:
      volumes:
        - name: persistent-storage
          persistentVolumeClaim:
            claimName: volume-claim-name
      containers:
        - name: web
          image: nginx
          ports:
            - name: web
              containerPort: 80
          volumeMounts:
            - name: persistent-storage
              mountPath: "/usr/share/nginx/html/"

## Appendix D : publish your images to ECR

If you prefer downloading images from AWS, you can create an Elastic Container Registry (ECR, https://aws.amazon.com/ecr/) and create 5 repositories named

 * terracotta-server
 * tmc
 * terracotta-cluster-tool
 * sample-ehcache-client
 * sample-tcstore-client

to push all your Terracotta images.
To login :

    aws ecr get-login

Make sure to execute the docker login command line that was returned.

Set a few environment variables now :


    # the url can be found after previous aws ecr get-login command, or from the AWS ECR UI
    export AWS_ECR_URL=xxxxx.dkr.ecr.ca-central-1.amazonaws.com
    # in case you'll use just 1 repository to store all your terracotta images
    export DOCKER_STORE_SAG=store/softwareag
    export TERRACOTTA_TAG=10.2

First, you'll need to pull all the images locally :

    docker pull $DOCKER_STORE_SAG/tmc:$TERRACOTTA_TAG
    docker pull $DOCKER_STORE_SAG/terracotta-server:$TERRACOTTA_TAG
    docker pull $DOCKER_STORE_SAG/sample-ehcache-client:$TERRACOTTA_TAG
    docker pull $DOCKER_STORE_SAG/sample-tcstore-client:$TERRACOTTA_TAG
    docker pull $DOCKER_STORE_SAG/terracotta-cluster-tool:$TERRACOTTA_TAG


After you have locally pulled the Terracotta images from the storeag them and push them to your AWS ECR registry :

    docker tag $DOCKER_STORE_SAG/terracotta-server:$TERRACOTTA_TAG $AWS_ECR_URL/terracotta-server:$TERRACOTTA_TAG
    docker tag $DOCKER_STORE_SAG/tmc:$TERRACOTTA_TAG $AWS_ECR_URL/tmc:$TERRACOTTA_TAG
    docker tag $DOCKER_STORE_SAG/sample-tcstore-client:$TERRACOTTA_TAG $AWS_ECR_URL/sample-tcstore-client:$TERRACOTTA_TAG
    docker tag $DOCKER_STORE_SAG/sample-ehcache-client:$TERRACOTTA_TAG $AWS_ECR_URL/sample-ehcache-client:$TERRACOTTA_TAG
    docker tag $DOCKER_STORE_SAG/terracotta-cluster-tool:$TERRACOTTA_TAG $AWS_ECR_URL/terracotta-cluster-tool:$TERRACOTTA_TAG

    docker push $AWS_ECR_URL/terracotta-server:$TERRACOTTA_TAG
    docker push $AWS_ECR_URL/tmc:$TERRACOTTA_TAG
    docker push $AWS_ECR_URL/sample-tcstore-client:$TERRACOTTA_TAG
    docker push $AWS_ECR_URL/sample-ehcache-client:$TERRACOTTA_TAG
    docker push $AWS_ECR_URL/terracotta-cluster-tool:$TERRACOTTA_TAG
