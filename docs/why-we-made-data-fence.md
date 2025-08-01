---
layout: default
title: Why We Made Data Fence
---

Zoom's Analytics Data Warehouse contains hundreds of thousands of objects with millions 
of total grants on objects to roles. As our analytics program matured, the need to manage 
multiple environments and multiple regions required that we manage our Database 
Security through source-controlled infrastructure as code.

We first attempted to use Terraform, which works very well for us for many other 
infrastructure tasks. However, we found that while Terraform could technically 
manage Snowflake grants, we were stretching Terraform's intended design. Even though Terraform 
is written in GoLang, Snowflake grants resulted in very large state files which degraded 
performance and made development painful. In addition, the fundamental assumptions 
of Terraform conflict with the SQL security use case. In most SQL security 
implementations, the objects must be created before grants are placed on them. If the 
object is dropped, all grants are lost. Tools like DBT frequently use drop and create for 
small models. In such a design, Terraform would expect to control these objects and not 
have them created or dropped outside of Terraform.

In addition, as we transitioned from manual control to infrastructure as code, we needed
a tool capable of self-healing. If an administrator accidentally made a change on a 
version-controlled object or if object lifecycle changes altered the grants, we need the 
tool to gracefully return the object to its desired configuration. By running the tool 
on the schedule, we can ensure that this happens soon after the mistake was made so that 
applications don't start to depend on the manual grants.

After having significant success at Zoom using Data Fence at scale since 2023, we have 
decided to release this tool open source.