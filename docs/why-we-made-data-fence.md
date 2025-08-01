---
layout: default
title: Why We Made Data Fence
---
Zoom's Analytics Data Warehouse manages hundreds of thousands of objects with millions of
total permissions granted to roles. As we expanded to multiple environments and regions, we needed a better
approach to security management.

## The Enterprise-Scale Challenge

Our growing data infrastructure demanded a systematic approach to security. Manual processes
simply could not provide the consistency and reliability we needed across our expanding
analytics ecosystem.

## Beyond Traditional Tools

We first tried Terraform, our go-to solution for infrastructure management. Unfortunately,
Terraform was not the right fit for our Snowflake security needs.

* **Performance Degradation**: The sheer volume of Snowflake grants created massive state files
  that slowed development and hampered our team's productivity.

* **Architectural Mismatch**: Terraform's design philosophy clashed with SQL security realities.
  In SQL environments, objects must exist before permissions are granted, and when objects are
  dropped, their permissions vanish too. This fundamental disconnect created ongoing friction
  with our development workflows.

## The Self-Healing Solution

We needed more than just automation. We needed resilience. Our solution had to address key
requirements for modern data security management.

* Automatically fix accidental permission changes made outside our control systems.
* Handle permission disruptions when underlying objects change during normal operations.
* Maintain desired security configurations through scheduled enforcement, preventing
  dependency on temporary manual changes.

## From Internal Success to Open Source

Since 2023, we have successfully used Data Fence to implement access control at enterprise
scale. Now we are sharing this solution as an open source tool so others can benefit from our
experience solving these complex data security challenges.
