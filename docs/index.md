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
exist outside your security infrastructure, maintaining protection without disrupting
operations.

### Current Support and Roadmap

Currently, Data Fence exclusively supports Snowflake data warehouse environments. With
sufficient community interest, support for additional data warehouse platforms is planned
for future releases.

### Deployment Recommendation

While Data Fence can integrate with standard continuous deployment workflows, optimal
results are achieved through scheduled execution. This approach enables continuous
monitoring and automatic remediation of permission discrepancies, ensuring your warehouse
maintains proper access controls at all times.
