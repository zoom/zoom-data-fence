---
layout: default
title: Zoom Data Fence
---


Data Fence provides robust data warehouse security management that ensures stable,
repeatable permission structures across your environment.

## Enterprise Data Warehouse Security Management

As AI and machine learning applications increasingly access sensitive data, Data Fence
creates the essential security layer that enables innovation while preventing unauthorized
access. By proactively correcting drift in role permissions, Data Fence ensures that 
both humans and autonomous systems operate reliably within intended boundaries. 

Without constant oversite, roles in SQL databases grow to become over-granted. Administrators
become reluctant to remove access that has existed for a long time for fear of breaking 
unknown processes. By revoking undesired access immediately, Data Fence prevents 
applications and users from becoming dependent on over grants.  

![High Level Data Flow]({{"/resources/data-fence-high-level-data-flow.png" | relative_url}}){: width="100%" height="auto" }

### Key Capabilities

* **Cross-Environment Consistency** — Deploy identical security configurations across
multiple environments, ensuring uniformity in your security posture.
* **Automatic Configuration Healing** — Proactively identifies and resolves permission drift
by revoking unauthorized grants and restoring missing ones. This addresses a critical
challenge in SQL-based data warehouses where stateful behaviors from object creation and
deletion can compromise security integrity.
* **Enterprise-Grade Scalability** — Efficiently manages millions of grants across hundreds
of thousands of objects—a scale that overwhelms traditional tools like Terraform.
* **Adaptive Object Management** — Seamlessly handles objects with transient lifecycles that
are managed outside your security infrastructure, maintaining protection without disrupting
operations.
* **Change Management and Approval** — By keeping all permissions in source control, 
approval, quality and release processes can be leveraged. 

### Current Support and Roadmap

Currently, Data Fence exclusively supports [Snowflake](https://www.snowflake.com) data warehouse environments. With
sufficient community interest, we can also support additional data warehouse platforms.

### Deployment Recommendation

While Data Fence can integrate with standard continuous deployment workflows which run
at deployment time, optimal results are achieved through scheduled execution. This approach 
enables continuous automatic remediation of permission discrepancies, ensuring 
your warehouse maintains proper access controls at all times.
