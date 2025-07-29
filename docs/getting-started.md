---
layout: default
title: Getting Started
---

You can use this system locally, in a Continuous Deployment (CD) pipeline or in an 
orchestration service with an image.

First create a programmatic access token on Snowflake so that you can login without a 
browser. Note that this application is for security administration and will therefore 
require elevated access.

You can try out the [example configuration](https://github.com/zoom/zoom-data-fence/tree/main/example).

```shell
git clone https://github.com/zoom/zoom-data-fence.git
cd zoom-data-fence/example
```

```sql
alter user MY_USER add programmatic access token dfence_test
role_restriction = 'ACCOUNTADMIN';
```

Export Environment Variables. These are based on the variables required in the
[profiles.yml](https://github.com/zoom/zoom-data-fence/blob/main/example/profiles.yml). You can specify your own variable names
based on your needs when you write your own profile.

```shell
export DFENCE_USER="Your Snowflake Login Name"
export DFENCE_TOKEN="Your Programmatic Access Token"
export DFENCE_ACCOUNT="Your Snowflake Account Name"
```

Run a compile command in Docker using the [example configuration](https://github.com/zoom/zoom-data-fence/tree/main/example).


```shell
docker run -it -v $PWD/example:/example -e DFENCE_TOKEN -e DFENCE_USER -e DFENCE_ACCOUNT \
  --workdir /example zoomvideo/zoom-data-fence \
  compile --var-file env/dev/vars.yml roles
```

Alternately, you can execute within a docker comtainer. 

```shell
docker run -it -v $PWD/example:/example -e DFENCE_TOKEN -e DFENCE_USER -e DFENCE_ACCOUNT \
  --workdir /example --entrypoint bash zoomvideo/zoom-data-fence
```

Now you shoudl have a shell in the docker container. You can run the compile command directly.

```shell
  dfence compile --var-file env/dev/vars.yml zoomvideo/zoom-data-fence
```

If you like the compiled result, you can instead run the apply command.

```shell
  dfence apply --var-file env/dev/vars.yml zoomvideo/zoom-data-fence
```

