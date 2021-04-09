This directory contains a hacky way to start terracotta in a pure-IPv6 environment for the purpose of
verifying basic compatibility.

Essentially, the Dockerfiles here simply extend the official docker images here
https://dtr.eur.ad.sag:4443/repositories/terracotta and prepend their entrypoints with a script

### Notes:
* All containers except tmc run in pure IPv6 (they do not have IPv4 addresses)
* TMC retains IPv4 so it can be remotely accessed

###Prerequisites:
* Remote or local Docker daemon:
  * In IPv6 mode (https://docs.docker.com/engine/userguide/networking/default_network/ipv6/)
  * Logged into dtr.eur.ad.sag:4443
* docker-compose installed (current minimum version: 1.13.0)

The docker-compose.yml file is self-contained and does not rely on externally created items except for
the terracotta images.  The ipv6 images are built at runtime

###(Re)Running:

    # -v is "also remove volume", this clears out hosts file data
    docker-compose down -v ; docker-compose build ; docker-compose up
    # check the output for errors and container termination.
    #
    # On success:
    docker ps # look for the public port for the TMC (in a new shell), and visit
        # http://DOCKER_HOST:PUBLIC_PORT

Note that it's possible to run this against a remote server by setting DOCKER_HOST


##Troubleshooting intermittent issues:
* "Address Already In Use" on startup: just retry
* "Network cannot be removed, in use by task X": restart docker and retry.
* Also try --verbose with the up command, that seems to make it work better.

