---
layout: default
title: Getting Started
---

We will demonstrate how to use Data Fence locally with a Docker container. In most real world 
use cases, you will probably be running similar commands inside of a Continuous Deployment 
pipeline or a cloud service inside of a Docker Container.

First create a programmatic access token on Snowflake so that you can login without a 
browser. Note that this application is for security administration and will therefore 
require elevated access.

## Clone the Example

You can try out the [example configuration](https://github.com/zoom/zoom-data-fence/tree/main/docs/_includes/example).

```shell
git clone https://github.com/zoom/zoom-data-fence.git
cd zoom-data-fence/docs/_includes/example
```

## Connect To Snowflake
You will need to be able to connect to Snowflake. The example uses a profiles.yml file
which defines your connection to Snowflake.

```yaml
{% include example/profiles.yml %}
```

You will need a programmatic method to connect to Snowflake. For this example, we are using
a [Programmatic Access Token](https://docs.snowflake.com/en/user-guide/programmatic-access-tokens).

```sql
alter user MY_USER add programmatic access token dfence_test
role_restriction = 'ACCOUNTADMIN';
```

Export Environment Variables for the variables in the `profiles.yml`file. You can specify 
your own variable names based on your needs when you 
[write your own profile]({% link profiles.md %}).

```shell
export DFENCE_USER="Your Snowflake Login Name"
export DFENCE_TOKEN="Your Programmatic Access Token"
export DFENCE_ACCOUNT="Your Snowflake Account Name"
```

# Define Snowflake Roles To Create
Now we will use a role definition file to define roles.

```yaml
{% include example/roles/example-role.yml %}
```

Note that the role file also refers to data fence variables. These variables could be 
provided by environment variables like we did with the connection variables. However,
they can also be provided by a file. It is convenient to organize variables into files 
for each environment. Here we have placed the environment file at env/dev/vars.yml.

```yaml
{% include example/env/dev/vars.yml %}
```
Run a compile command using Docker.

```shell
docker run -it -v $PWD:/example -e DFENCE_TOKEN -e DFENCE_USER -e DFENCE_ACCOUNT \
  --workdir /example zoomvideo/zoom-data-fence \
  compile --var-file env/dev/vars.yml roles
```

You should now see the compiled changes which would be run.

```shell
---
changes:
- role-creation-statements:
  - "CREATE ROLE IF NOT EXISTS EXAMPLE_1_DEV;"
  - "GRANT OWNERSHIP ON ROLE EXAMPLE_1_DEV TO ROLE securityadmin COPY CURRENT GRANTS;"
  role-grant-statements: []
  role-id: "example-1"
  role-name: "example_1_dev"
- role-creation-statements:
  - "CREATE ROLE IF NOT EXISTS EXAMPLE_2_DEV;"
  - "GRANT OWNERSHIP ON ROLE EXAMPLE_2_DEV TO ROLE securityadmin COPY CURRENT GRANTS;"
  role-grant-statements:
  - - "GRANT ROLE \"EXAMPLE_1_DEV\" TO ROLE EXAMPLE_2_DEV;"
  role-id: "example-2"
  role-name: "example_2_dev"
total-changes: 2
total-roles: 2
```


Alternately, you can execute within a docker container. 

```shell
docker run -it -v $PWD:/example -e DFENCE_TOKEN -e DFENCE_USER -e DFENCE_ACCOUNT \
  --workdir /example --entrypoint bash zoomvideo/zoom-data-fence
```

Now you should have a shell in the docker container. You can run the compile command
directly.


```shell
dfence compile --var-file env/dev/vars.yml roles
```

If you like the compiled result, you can instead run the apply command.

```shell
dfence apply --var-file env/dev/vars.yml roles
```

You are asked to approve the changes. Select 'y' if you are Ok with the roles being 
created.

If you run compile again, you should see that no further changes are planned.

```yaml
---
changes: []
total-changes: 0
total-roles: 2
```

# Evolve Roles
No let's edit the roles file to remove example_2_dev role's usage permission 
on the example_1_dev role.

```yaml
{% include example/roles-changed/example-role.yml %}
```

Let's run apply against the changed role.

```shell
dfence apply --var-file env/dev/vars.yml roles-changed
```

Now we should see that Data Fence will revoke the grant.

```shell
---
changes:
- role-creation-statements: []
  role-grant-statements:
  - - "REVOKE ROLE \"EXAMPLE_1_DEV\" FROM ROLE EXAMPLE_2_DEV;"
  role-id: "example-2"
  role-name: "example_2_dev"
total-changes: 1
total-roles: 2
```

We can [manage permissions for roles]({% link defining-roles.md %}) on any snowflake object with Data Fence, such as tables, 
views, schemas, procedures, functions and database.