# Zoom Data Fence (dfence)

Data Fence is a Data Warehouse Security Management tool which helps database
administrators keep data warehouse permissions stable, repeatable and easy to manage.

1. Repeat the same security configuration across multiple environments.
2. Automatically heal configuration drift by revoking undesired grants and granting 
missing grants. This is a significant problem in SQL based data warehouses which have 
stateful implicit behavior resulting from behavior such as creating objects and 
dropping objects.
3. Scale to manage millions of grants on hundreds of thousands of objects. Terraform 
chokes on the scale of data warehouse grants.
4. Gracefully handle the transient existance of objects controlled outside of the security 
infrastructure.

Currently, Data Fence only supports the [Snowflake](https://www.snowflake.com/en/) data 
warehouse. However, with enough community support, it can support additional data 
warehouses in the future. 

While Data Fence can be run at deploy time like other continuous deployment tools, 
we have found that it works best when you run it on a schedule so that it is continuously 
analyzing the grants in the warehouse, revoking undesired grants and granting missing 
grants.

For additional information, please see the [Documentation](https://zoom.github.io/zoom-data-fence/).

## Why We Made Data Fence
Zoom's Analytics Data Warehouse contains hundreds of thousands of objects with millions 
of total grants on objects to roles. As our analytics program matured, the need to manage 
multiple environments and multiple regions required that we manage our Database 
Security through source-controlled infrastructure as code. 

We first attempted to use Terraform, which works very well for us for many other 
infrastructure tasks. However, we found that while Terraform could technically 
manage Snowflake grants, we where stretching it's intended design. Even though Terraform 
is written in GoLang, Snowflake grants resulted in very large state files which degraded 
performance and made development painful. In addition, the fundamental assumptions 
of Terraform conflict with the SQL security use case. In most SQL security 
implementations, the objects must be created before grants are placed on them. If the 
object is dropped, all grants are lost. Tools like DBT frequently use drop and create for 
small models. In such a design, Terraform would expect to control these objects and not 
have them created or dropped outside of Terraform. 

In addition, as we transitioned from manual control to infrastructure as code, we needed
our tool which emphasizes self healing. If an administrator accidentally made a change on a 
version controlled object or if object lifecycle changes altered the grants, we need the 
tool to gracefully return the object to it's desired configuration. By running the tool 
on the schedule, we can ensure that this happens soon after the mistake was made so that 
applications don't start to depend on the manual grants.

After having significant success at Zoom using Data Fence at scale since 2023, we have 
decided to release this tool open source. 

## Contributing
We need contributors! As we move Data Fence from an internal to an open source tool, we 
need community contributors who can add the features necessary to make the tool more 
broadly applicable. Please view the [CONTRIBUTING.md](./CONTRIBUTING.md) file for 
information on contributing.

## Usage With Image

You can use this system locally, in a Continuous Deployment (CD) pipeline or in an 
orchestration service with an image. 

First create a programmatic access token on Snowflake so that you can login without a 
browser. Note that this application is for security administration and will therefore 
require elevated access.

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

#### Demo Setup

In order to demonstrate the tool, let's create some objects to grant permissions on.

```sql
create database if not exists my_db;
create schema if not exists my_db.my_schema;
create table if not exists my_db.my_schema.my_table(idx integer);
```

#### Roles File

Create a file with a configuration using some databases which are under your control.

```yaml
# local/roles.yml
roles:
  rbac-example-role-1:
    name: rbac_example_1
    create: true
    grants:
      - database-name: my_db
        object-type: database
        privileges:
          - usage
      - database-name: my_db
        schema-name: my_schema
        object-type: schema
        privileges:
          - usage
      - database-name: my_db
        schema-name: my_schema
        object-name: my_table
        object-type: table
        privileges:
          - select
  rbac-example-role-2:
    name: rbac_example_2
    create: true
    grants:
      - object-type: role
        privileges:
          - USAGE
        object-name: rbac_example_1
```

#### Profiles File

Next you will need to create a profiles file that you can use to connect to the database.
The profiles file can list multiple environments. This is a file for your filesystem only.

```yaml
# local/profiles.yml
default-profile: dev
profiles:
  dev:
    provider-name: SNOWFLAKE
    connection:
      snowflake:
        connection-string: jdbc:snowflake://your-dev-account.snowflakecomputing.com:443
        connection-properties:
          authenticator: externalbrowser
          user: YOUR.USERNAME
          role: SECURITYADMIN
  prod:
    provider-name: SNOWFLAKE
    connection:
      snowflake:
        connection-string: jdbc:snowflake://your-prod-account.snowflakecomputing.com:443
        connection-properties:
          authenticator: externalbrowser
          user: YOUR.USERNAME
          role: SECURITYADMIN
```

Note that to use this tool you will need SECURITYADMIN or a similar role with enough
permissions to create roles and manage grants.

#### Execution

Now you can compile your changes into a plan.

```shell
dfence compile --profile-file local/profiles.yml local/roles.yml
```

You should see a set of changes that are planned.

```shell
---
changes:
- role-creation-statements:
  - "create role if not exists rbac_example_2;"
  role-grant-statements:
  - "grant role rbac_example_1 to role rbac_example_2;"
  role-id: "rdbac-example-role-2"
  role-name: "rbac_example_2"
- role-creation-statements:
  - "create role if not exists rbac_example_1;"
  role-grant-statements:
  - "grant usage on database my_db to role rbac_example_1;"
  - "grant usage on schema my_db.my_schema to role rbac_example_1;"
  - "grant select on table my_db.my_schema.my_table to role rbac_example_1;"
  role-id: "rdbac-example-role-1"
  role-name: "rbac_example_1"
total-roles: 2
total-changes: 2

```

If you would like to execute the changes, you can apply them.

```shell
dfence apply --profile-file local/profiles.yml local/roles.yml
```

Now that we have made the changes, we can execute the compile step again. We should see no
changes.

```shell
dfence compile --profile-file local/profiles.yml local/roles.yml
```

```yaml
---
changes: [ ]
total-roles: 2
total-changes: 0
```

data-fence strives to not run any statements against the database unless they are
actually needed. This reduces noise when one needs to look at system logs to investigate a
change. In addition, note that data-fence will remove grants which are not defined in
the roles file. Let's try manually granting a permission outside the process.

```sql
grant create schema on database my_db to role rbac_example_1;
```

Now let's run the tool again.

```shell
dfence apply --profile-file local/profiles.yml local/roles.yml
```

We now see that we are revoking the rogue grant.

```shell
---
changes:
- role-creation-statements: []
  role-grant-statements:
  - "revoke create schema on database my_db from role rbac_example_1;"
  role-id: "rdbac-example-role-1"
  role-name: "rbac_example_1"
total-roles: 2
total-changes: 1
```

This ability to revoke grants by deleting them from the configuration is a feature.
However, it is important to note that if you are migrating roles to data-fence, you need
to include all the grants already in the backend. Prior to deployment, you should run a
compile command and see no changes. Only at this point should you make additional changes.

#### Clean Up

Now we should clean up the objects that we made in our example.

```sql
use role sysadmin;
drop database if exists my_db;
use role securityadmin;
drop role if exists rbac_example_1;
drop role if exists rbac_example_2;
drop role if exists rbac_example_3;
```

#### Variables

Variables can be used in profile files and roles files. Create a variables file like this.

```yaml
# variables-dev.yml
env-suffix=dev
src-database-name=src_experimental
sso-finance-data-analyst-role=okta_finance_analyst_dev
```

Now you can use these variables in the files.

```yaml
# roles.yml
roles:
  rbac-example-role-1:
    name: rbac_example_1${var.env-suffix}
    grants:
      - database-name: ${var.src-database}
        object-type: database
        privileges:
          - usage
      - object-type: role
        object-name: ${var.sso-finance-data-analyst-role}
        privileges:
          - usage
```

You can pass these variables using the `--var-file` option.

```shell
dfence apply --profile-file local/profiles.yml --var-file local/variables-dev.yml local/roles.yml
```

### Wildcard Grants

Wildcards may be used to perform grants on all future and existing objects within a schema
or database. For example, this permission will grant `select` on all future and existing
tables within database `my_db` to role `rbac_example_1`.

```yaml
# local/roles.yml
roles:
  rbac-example-role-1:
    name: rbac_example_1
    grants:
      - database-name: my_db
        object-name: "*"
        object-type: table
        privileges:
          - select
```

There are times whwn you may wish to only affect future roles or current roles. By 
default, both are included in a wildcard grant. 

For example, if you wish to leave existing grants alone but you want any new tables to 
have the grant, then you would set `include-future: true` and `include-all: false`. 

```yaml
# local/roles.yml
roles:
  rbac-example-role-1:
    name: rbac_example_1
    grants:
      - database-name: my_db
        object-name: "*"
        object-type: table
        include-future: true
        include-all: false
        privileges:
          - select
```

### Ownership Grants

All ownership grants are implemented in
[Snowflake with Copy Current Grants](https://docs.snowflake.com/en/sql-reference/sql/grant-ownership).
In addition, when an ownership grant is destroyed, rather than being revoked it is granted
to the configured SECURITYADMIN role with copy current grants. This prevents other grants on the
object from being destroyed during the change of ownership. 

However, such an outcome is not desirable. When such ownership revocations occur, they 
are an indication that object ownership needs to be explicitly assigned to a role in the 
configuration.

### Importing Roles

You can import existing roles in order to make a new playbook.

```shell
dfence import-roles role_a role_b
```

In addition, it is possible to search for roles based on a regular expression. For
example, in order to find all roles which start with UR_ or SR_, use the following
command.

```shell
dfence import-roles --patterns '^UR_.*' '^SR_.*'
```

### Multiple Roles Files

Multiple role files can be used by providing a directory instead of a file path. If a
directory is provided, all files with a "yml" or "yaml" file extension will be discovered.

### Granting Permissions To Roles Created Another Way

Most of the time, it is best to have all roles completely managed by the RBAC tool.
However, there are times, such as SCIM provisioning, when it is necessary to perform
grants to roles that are created another way. In these cases, we want to make sure that if
the role does not exist, we do not create it and thus cause problems with ownership or
mask problems in the provisioning system. The `create` property can be set to false if you
do not desire to create a role.

```yaml
# roles.yml
roles:
  rbac-example-role-1:
    name: rbac_example_1
    create: false
    grants:
      - database-name: my_db
        object-type: database
        privileges:
          - usage
```

### Granting Permissions To Roles Without Revoking Grants Provided Another Way

While it is best for data-fence to fully own the permissions for a role, in some cases,
it is necessary to use the tool to grant permissions without revoking the permissions
granted another way. This may be necessary while migrating ownership of objects or while
migrating from another system. In these cases, the `revoke-other-grants`
attribute may be set to `false`.

```yaml
# roles.yml
roles:
  rbac-example-role-1:
    name: rbac_example_1
    revoke-other-grants: false
    grants:
      - database-name: my_db
        object-type: database
        privileges:
          - usage
```

## Unsupported Grants

Certain features and grants are not supported. These include.

* Snowflake Imported Privileges on Shares
* Certain Snowflake ML Grants
* New grant types for features that have not yet been mapped in this application.

