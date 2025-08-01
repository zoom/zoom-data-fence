---
layout: default
title: Defining Roles
---

The playbook YAML files are the core configuration file for Data Fence. They defines the roles and their permissions
that will be managed by the tool. You can define as many playbook files as you like. The files will all be merged when
you run compile or apply.

## Structure

The playbook files have the following top-level structure:

```yaml
role-owner: SECURITYADMIN  # Optional
roles:
  data-analyst-role:
    name: data_analyst
    grants:
      - object-type: database
        object-name: analytics_db
        privileges:
          - usage
      - object-type: schema
        database-name: analytics_db
        schema-name: public
        privileges:
          - usage
      - object-type: table
        database-name: analytics_db
        schema-name: public
        object-name: "*"
        privileges:
          - select
```

## Fields

### Top-Level Fields

| Field      | Type   | Required | Default | Description                                                     |
|------------|--------|----------|---------|-----------------------------------------------------------------|
| roles      | Map    | Yes      | -       | A map of role IDs to role [configurations](#role-config-fields) |
| role-owner | String | No       | -       | The default owner for all roles in the playbook                 |

### Role Configuration Fields {#role-config-fields}

Each role in the `roles` map has the following fields:

| Field               | Type    | Required | Default | Description                                                  |
|---------------------|---------|----------|---------|--------------------------------------------------------------|
| name                | String  | Yes      | -       | The name of the role in the database                         |
| grants              | List    | No       | []      | A list of [privilege grants](#privilege-grants) for the role |
| create              | Boolean | No       | true    | Whether to create the role if it doesn't exist               |
| revoke-other-grants | Boolean | No       | true    | Whether to revoke grants not defined in the playbook         |
| enable              | Boolean | No       | true    | Whether to enable the role                                   |
| role-owner          | String  | No       | -       | The owner of the role, overrides the top-level role-owner    |

### Privilege Grant Fields {#privilege-grants}

Each item in the `grants` list has the following fields:

| Field          | Type    | Required     | Default | Description                                                                     |
|----------------|---------|--------------|---------|---------------------------------------------------------------------------------|
| object-type    | String  | Yes          | -       | The type of object to grant privileges on (e.g., database, schema, table, role) |
| object-name    | String  | No           | -       | The name of the object to grant privileges on                                   |
| schema-name    | String  | No           | -       | The name of the schema containing the object                                    |
| database-name  | String  | No           | -       | The name of the database containing the schema or object                        |
| privileges     | List    | Yes          | -       | A list of privileges to grant (e.g., select, usage, create)                     |
| include-future | Boolean | No           | true    | Whether to include future objects in the grant                                  |
| include-all    | Boolean | No           | true    | Whether to include all existing objects in the grant                            |
| enable         | Boolean | No           | true    | Whether to enable the privilege grant                                           |

At least one of `object-name`, `schema-name`, or `database-name` must be provided, except for account-level grants.

## Role with Wildcard Grants
Most of the time, we don't want to define every single grant. Instead, we want to 
grant a role a privilege on every object within a database or schema. In these cases we 
can use wildcard grants using the "*" character in the `object-name` or `schema-name`
field.

```yaml
roles:
  data-engineer-role:
    name: data_engineer
    grants:
      - object-type: database
        object-name: raw_data
        privileges:
          - usage
      - object-type: schema
        database-name: raw_data
        schema-name: "*"
        privileges:
          - usage
      - object-type: table
        database-name: raw_data
        schema-name: "*"
        object-name: "*"
        include-future: true
        include-all: true
        privileges:
          - select
          - insert
          - update
```

There are times whwn you may wish to only affect future roles or current roles. By
default, both are included in a wildcard grant.

For example, if you wish to leave existing grants alone but you want any new tables to
have the grant, then you would set `include-future: true` and `include-all: false`.

```yaml
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

## Role without Auto-Creation
In some cases, if a role exists, we want to manage it's grants. However, we don't 
want to create the role if it does not exist. In these cases, we set `create` to false.

```yaml
roles:
  external-role:
    name: external_system_role
    create: false
    grants:
      - object-type: database
        object-name: shared_data
        privileges:
          - usage
```

## Role Without Revoking Other Grants

Sometimes we may want to add certain grants to a role while still allowing other grants 
to exist. This usually happens when we are transitioning a manually managed role to 
partial managemnet under Data Fence. We can do this by setting `revoke-other-grants`
to false.

```yaml
roles:
  hybrid-role:
    name: hybrid_managed_role
    revoke-other-grants: false
    grants:
      - object-type: database
        object-name: app_data
        privileges:
          - usage
```

## Multiple Roles Files

Multiple role files can be used by providing a directory instead of a file path. If a
directory is provided, all files with a "yml" or "yaml" file extension will be discovered.

## Granting Permissions To Roles Created Another Way

Most of the time, it is best to have all roles completely managed by the RBAC tool.
However, there are times, such as SCIM provisioning, when it is necessary to perform
grants to roles that are created another way. In these cases, we want to make sure that if
the role does not exist, we do not create it and thus cause problems with ownership or
mask problems in the provisioning system. The `create` property can be set to false if you
do not desire to create a role.

```yaml
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

## Granting Permissions To Roles Without Revoking Grants Provided Another Way

While it is best for data-fence to fully own the permissions for a role, in some cases,
it is necessary to use the tool to grant permissions without revoking the permissions
granted another way. This may be necessary while migrating ownership of objects or while
migrating from another system. In these cases, the `revoke-other-grants`
attribute may be set to `false`.

```yaml
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

## Ownership Grants

All ownership grants are implemented using
[Snowflake with Copy Current Grants](https://docs.snowflake.com/en/sql-reference/sql/grant-ownership).
In addition, when an ownership grant is destroyed, rather than being revoked it is granted
to the configured SECURITYADMIN role with copy current grants. This prevents other grants on the
object from being destroyed during the change of ownership.

However, such an outcome is not desirable. When such ownership revocations occur, they
are an indication that object ownership needs to be explicitly assigned to a role in the
configuration.

## Unsupported Grants
Certain features and grants are not supported. These include.

* Snowflake Imported Privileges on Shares
* New grant types for features that have not yet been mapped in this application.

