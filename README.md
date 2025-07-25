# zoom-data-fence

This is the future location of the Zoom Data Fence project. This is a placeholder for now.

## Usage With Docker

You can use this system locally with docker. 

First create a programmatic access token on Snowflake. Note that this application is for 
security administration and will therefore require elevated access.

```snowflake
alter user MY_USER add programmatic access token dfence_test
role_restriction = 'ACCOUNTADMIN';
```

Export Environment Variables. These are based on the variables required in 
[example/profiles.yml](./example/profiles.yml). You can specify your own variable names
based on your needs when you write your own profile.

```shell
export DFENCE_USER="<Your Snowflake Login Name>"
export DFENCE_TOKEN="<Your Programmatic Access Token>"
export DFENCE_ACCOUNT="<Your Snowflake Account Name>"
```

Run a compile command in Docker using the [example configuration](./example).
```shell
docker run -it -v $PWD/example:/example -e DFENCE_TOKEN -e DFENCE_USER -e DFENCE_ACCOUNT \
  --workdir /example zoomvideo/zoom-data-fence \
  compile --var-file env/dev/vars.yml roles 
```

