---
layout: default
title: Connecting to a Database
---

The profiles file is used to configure your connection to the database.

## Structure

The profiles file has the following top-level structure:

```yaml
default-profile: dev
profiles:
  dev:
    provider-name: SNOWFLAKE
    connection:
      snowflake:
        connection-string: jdbc:snowflake://your-dev-account.snowflakecomputing.com:443
        connection-properties:
          user: YOUR_USER
          private-key-file: /path/to/file
```

## Fields

### Top-Level Fields

The following top level fields are supported.

| Field | Type | Required | Default | Description |
| ----- | ---- | -------- | ------- | ----------- |
| default-profile | String | Yes | - | The profile to use as default. |
| profiles | Object | No | {} | Object with profile names as keys and [profile objects as values.](#profile-fields) |

<div id="profile-fields"></div>

### Profile Fields

| Field | Type | Required | Default | Description |
| ----- | ---- | -------- | ------- | ----------- |
| provider-name | One of [SNOWFLAKE] | No | SNOWFLAKE | Chose which provider to use. |
| connection | | No | - | Provider specific connection properties. |

### Connection Fields

The connection field corresponding to the provider name is required.

| Field | Type | Required | Default | Description |
| ----- | ---- | -------- | ------- | ----------- |
| snowflake | Object | No | - | [Snowflake connection properties.](#snowflake-connection-fields) |


### Snowflake Connection Fields
<div id="snowflake-connection-fields"></div>

| Field | Type | Required | Default | Description |
| ----- | ---- | -------- | ------- | ----------- |
| connection-string | String | Yes | - | The JDBC connection string for the database. |
| connection-properties | Object | Yes | - | [Snowflake connection properties.](#snowflake-connection-properties-fields) |


<div id="snowflake-connection-properties-fields"></div>

### Snowflake Connection Properties Fields

Most connection properties pass directly through to the [Snowflake JDBC driver](https://docs.snowflake.com/en/developer-guide/jdbc/jdbc-parameters).

| Field | Type | Required | Default | Description |
| ----- | ---- | -------- | ------- | ----------- |
| user | String | Yes | - | Snowflake user login name. Note that in Snowflake the login name is not always the same as the user name. |
| authenticator | String | No | - | Snowflake authenticator. |
| passcode | String | No | - | Specifies the passcode to use for multi-factor authentication. |
| passcode-in-password | One of [on, off] | No | - | Specifies whether the passcode for multi-factor authentication is appended to the password. |
| password | String | No | - | User password. The password field may also be used for [programmatic access tokens.](https://docs.snowflake.com/en/user-guide/programmatic-access-tokens) |
| token | String | No | - | Specifies the OAuth token to use for authentication, where <string> is the token. This parameter is required only when setting the authenticator parameter to oauth, except as noted below. |
| security-admin-role | String | No | SECURITYADMIN | Specify which role to use for security operations such as creating roles and performing grants. |
| sys-admin-role | String | No | SYSADMIN | Specify which role to use for SYSADMIN operations such as finding all of the objects in a database. |
| warehouse | String | No | - | Snowflake role. |
| log-in-timeout | Number | No | 60 | Log in timeout. |
| network-timeout | Number | No | 0 | Network timeout. |
| query-timeout | Number | No | 0 | Query timeout. |
| max-connections | Number | No | 300 | Max connections. |
| max-connections-per-route | Number | No | 300 | Max connections per route.. |
| private-key-base64 | String | No | - | Private key encoded as a base64 string. |
| private-key-file | String | No | - | Private key file path. |
| private-key-pwd | String | No | - | Private key password. |

## Using Variables In Profiles
The profiles file supports use of Variables.

This is useful for injecting secret values into the profiles file such as private keys and passwords.

```yaml
default-profile: local
profiles:
  local:
    provider-name: SNOWFLAKE
    connection:
    snowflake:
    connection-string: jdbc:snowflake://${var.account}.snowflakecomputing.com:443
    connection-properties:
      user: ${var.user}
      password: ${var.token}
```

However, note that variables are processed BEFORE the profiles file is
parsed and a specific profile is selected. Therefore, if there are variables present in
one environment but not another, values will need to be provided for all environments.

In the following example, both the var.token and var.private-key-base-64 variables are
required to be provided for both profiles even though the local profile only needs token and the
deploy profile only needs private-key-base-64.

```yaml
default-profile: local
profiles:
  local:
    provider-name: SNOWFLAKE
    connection:
    snowflake:
      connection-string: jdbc:snowflake://${var.account}.snowflakecomputing.com:443
      connection-properties:
      user: ${var.user}
      password: ${var.token}
  deploy:
    provider-name: SNOWFLAKE
    connection:
    snowflake:
      connection-string: jdbc:snowflake://${var.account}.snowflakecomputing.com:443
      connection-properties:
      user: ${var.user}
      private-key-base64: ${var.private-key-base-64}
```

If this behavior is not desired, separate files can be used instead by specifying the profiles file to use at the
command line.

```shell
export DFENCE_USER=MY_USER
export DFENCE_TOKEN=MY_TOKEN
export DFENCE_ACCOUNT=MY_ACCOUNT
dfence compile --profiles-file /env/local/profiles.yml
```