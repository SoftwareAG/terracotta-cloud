# Smoke test for Terracotta Docker images

After you've locally build the images, you want to quickly check whether they seem to be working or not.

## What those scripts do

Those scripts will help you find out, they basically:

* Run both the Terracotta Servers in cluster
* Run a Terracotta Management Console (TMC)
* Assert the tmc "sees" the servers and its resources
* Run an ehcache client and assert the TMC can "see" the caches

## How to run them

```bash
export VERSION=4.3.8

Downloaf the tar ball
tar -xvf <tar-ball>

cd <unzipped Folder>

cp -r terracotta-cloud/docker/ ./docker

docker build --file docker/images/server/Dockerfile --tag terracotta:$VERSION .

docker build --file docker/images/tmc/Dockerfile --tag tmc:$VERSION .

docker build -f docker/images/client/Dockerfile --tag ehcache-client:$VERSION .

Put license file terracotta-license.key in <unzipped folder>

./smoke-test.sh
```