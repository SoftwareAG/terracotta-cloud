#### What is the the websessions example ?

Terracotta Server supports a distributed in-memory data-storage topology, which enables the sharing of data among multiple caches and in-memory

Web Sessions allows sticky and non-sticky (depending on each container support) session clustering on top of Terracotta products.

The cart example is a very simple webapp that uses Web Sessions to provide sessions clustering to Tomcat.

#### How to use this image: QuickStart

You can run a terracotta server using (provided you built the terracotta image) :

    docker run -e ACCEPT_EULA=Y -d -p 9410:9410 --name terracotta terracotta-server:10.4.0-SNAPSHOT

Don't forget to install a license using the cluster tool :

    docker run -e ACCEPT_EULA=Y --link terracotta:terracotta -e "LICENSE_URL=https://server/license.xml" terracotta-cluster-tool:10.4.0-SNAPSHOT configure -n MyCluster -s terracotta

and then try running the sessions example, with :

    docker run -e JAVA_OPTS="-Dterracotta.websessions.store.tcstore.uri=terracotta://terracotta:9410/sessions-example -Dterracotta.websessions.store.tcstore.offheapResource=offheap-1 -Dterracotta.websessions.store.tcstore.diskResource=dataroot-1"  \
     -e ACCEPT_EULA=Y \
     --link terracotta:terracotta \
     -p 8080:8080 \
     -d websessions-cart-example:10.4.0-SNAPSHOT

and checkout what's happening with :

    docker logs -f sessions


#### How to configure a Kubernetes ingress to load balance to several pods

[Tested on Azure](https://docs.microsoft.com/en-us/azure/aks/ingress-basic) : create an ingress controller, and then configure it for the sessions service


    helm install stable/nginx-ingress --namespace kube-system --set controller.replicaCount=2 --set rbac.create=false

Then apply such a yaml configuration :

    apiVersion: extensions/v1beta1
    kind: Ingress
    metadata:
      name: sessions-ingress
      annotations:
        kubernetes.io/ingress.class: nginx
        nginx.ingress.kubernetes.io/ssl-redirect: "false"
    #    nginx.ingress.kubernetes.io/affinity: "cookie"
        nginx.ingress.kubernetes.io/rewrite-target: /
    spec:
      rules:
      - http:
          paths:
          - path: /
            backend:
              serviceName: sessions
              servicePort: 8080

An ingress controller being L7 , you can enable session affinity using the property : nginx.ingress.kubernetes.io/affinity: "cookie"

#### How to build this image

To build this Dockerfile

    $ cd terracotta-db-10.4.0-SNAPSHOT
    $ docker build --file docker/images/websessions-cart-example/Dockerfile --tag websessions-cart-example:10.4.0-SNAPSHOT .