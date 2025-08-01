---
layout: default
title: Importing Roles
---

You can import existing roles in order to make a new playbook.

```shell
dfence import-roles role_a role_b
```

In addition, it is possible to search for roles based on a regular expression. For
example, in order to find all roles that start with UR_ or SR_, use the following
command.

```shell
dfence import-roles --patterns '^UR_.*' '^SR_.*'
```

