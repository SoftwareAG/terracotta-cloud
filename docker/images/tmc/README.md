#What is the Terracotta Management Console?

The Terracotta Management Console (TMC) is a web-based administration and monitoring application allowing you to monitor and manage your Terracotta products (Ehcache, Terracotta Server Array)

#How to build this image

Once Docker is up and running in your environment, from the root folder (/opt/softwareag for example) run this command :

    docker build --file docker/images/tmc/Dockerfile --tag tmc:$VERSION .

#How to use this image: QuickStart

You can now run the TMC in a container :

- Using external license
    - Create <license-directory> having `license.key` file

        
    docker run -d -p 9889:9889 -v <license-directory>:/licenses --name tmc tmc:$VERSION

At this point go to http://localhost:9889/ from the host machine to see the TMC welcome page

By default the TMC will run with security disabled (no authentication, anonymous user have all roles)

If you want to change those default settings, [click on the "settings" button](https://terracotta.org/generated/4.3.0/html/bmm-all/#page/BigMemory_Max_Documentation_Set%2Fco-use_connections_and_settings.html%23wwconnect_header).

If you're invited to restart the TMC while changing the security configuration, just restart your container :

    docker restart tmc

And go back to http://localhost:9889

#How to use this image: with a Terracotta Server

Of course, having a TMC running only makes sense if you have a Terracotta Server to monitor / manage.

If you have already built the terracotta docker image, then, start it :

    docker run -d -p 9510:9510 --name terracotta terracotta

and then try running the tmc, with :

     docker run -d -p 9889:9889 -e ACCEPT_EULA=Y -v /path/to/license-folder:/licenses --name tmc tmc:$VERSION

and checkout what's happening with :

    docker logs -f tmc

Now you can create a new connection, and use http://terracotta:9540 as your Terracotta server URL

Or else, I suggest you to move on to the orchestration folder and use Docker compose to orchestrate your containers.