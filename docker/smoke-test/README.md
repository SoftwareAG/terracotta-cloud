# Smoke test for Terracotta Docker images

After you've locally build the images, you want to quickly check whether they seem to be working or not.

## What those scripts do

Those scripts will help you find out, they basically:

* Run a Terracotta Server
* Configure it with the config tool
* Run a Terracotta Management Console (TMC)
* Assert the tmc "sees" the servers and its resources
* Run an ehcache client and assert the TMC can "see" the caches
* Run an store client and assert the TMC can "see" the datasets

## How to run them

```bash
export VERSION=10.11.0-SNAPSHOT
./smoke-test.sh
```