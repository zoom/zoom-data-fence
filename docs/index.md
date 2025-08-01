---
layout: default
title: Zoom Data Fence
---

Data Fence is a Data Warehouse Security Management tool that helps database
administrators keep data warehouse permissions stable, repeatable and easy to manage.

* Repeat the same security configuration across multiple environments.
* Automatically heal configuration drift by revoking undesired grants and granting 
  missing grants. This is a significant problem in SQL-based data warehouses which have
  stateful implicit behavior resulting from behavior such as creating objects and 
  dropping objects.
* Scale to manage millions of grants on hundreds of thousands of objects. Terraform 
  chokes on the scale of data warehouse grants.
* Gracefully handle the transient existence of objects controlled outside of the security
  infrastructure.

Currently, Data Fence only supports the [Snowflake]("https://www.snowflake.com/en/">Snowflake) data 
warehouse. However, with enough community support, it can support additional data 
warehouses in the future.

While Data Fence can be run at deployment time like other continuous deployment tools, 
we have found that it works best when you run it on a schedule so that it is continuously 
analyzing the grants in the warehouse, revoking undesired grants and granting missing 
grants.
