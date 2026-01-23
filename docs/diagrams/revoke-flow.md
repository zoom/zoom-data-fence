# Revoke Package Flow

```mermaid
flowchart LR
    A[PlaybookPrivilegeGrants] --> B[Create Index]
    B --> C[GrantRevocationEvaluator]
    D[Current Grants] --> C
    C --> E{Grant Matches<br/>Playbook?}
    E -->|No| F[Mark for Revoke]
    E -->|Yes| G[Keep Grant]
    F --> H[Sorted Revoke List]
    
    style A fill:#e1f5ff
    style D fill:#e1f5ff
    style H fill:#c8e6c9
    style F fill:#ffcdd2
```

## Overview

The revoke package identifies which existing grants should be revoked by comparing current grants against playbook grants.

1. **Index Creation**: Playbook grants are indexed by object type and privilege for efficient lookup
2. **Evaluation**: Each current grant is evaluated to determine if it matches any playbook grant
3. **Matching**: Grants that don't match any playbook grant are marked for revocation
4. **Output**: Returns sorted list of grant builders to revoke

