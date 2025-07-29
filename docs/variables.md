---
layout: default
title: Variables
---

Variables can be used in profile files and roles files. Variables can come from files
or from environment variables.

# Variables From Roles Files
Create a variables file like this.

```yaml
env-suffix: dev
src-database-name: src_experimental
sso-finance-data-analyst-role: auth0_finance_analyst_dev
```

Now you can use these variables in the files.

```yaml
roles:
  rbac-example-role-1:
    name: rbac_example_1${var.env-suffix}
    grants:
      - database-name: ${var.src-database-name}
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

# Variables From Environment Variables
Variables can also come from environment variables. Environment variables should use 
UPPER_SNAKE_CASE prefixed by DFENCE_. For example, the following variables are equivalent.

```shell
export DFENCE_SRC_DATABASE=src_experimental
export DFENCE_ENV_SUFFIX=dev
export DFENCE_SSO_FINANCE_DATA_ANALYST_ROLE=auth0_finance_analyst_dev
```
