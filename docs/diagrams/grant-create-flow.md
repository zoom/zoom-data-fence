# Grant Create Package Flow

```mermaid
flowchart LR
    A[PlaybookPrivilegeGrant] --> B{Grant Type}
    B -->|Standard| C[Standard Grants]
    B -->|Container| D[Container Grants]
    C --> E[Create Grant Models]
    D --> F{Future/All}
    F -->|Future| G[Future Grants]
    F -->|All| H[Expand to Existing Objects]
    F -->|Both| I[Both Future & All]
    G --> E
    H --> E
    I --> E
    E --> J[Grant Builders]
    
    style A fill:#e1f5ff
    style J fill:#c8e6c9
    style D fill:#fff9c4
```

## Overview

The grant.create package generates desired grants from playbook privilege grants.

1. **Standard Grants**: Direct grants on specific objects (validates and qualifies object names)
2. **Container Grants**: Grants on containers (database/schema) that expand to:
   - **Future Grants**: Grants on future objects using `<OBJECT_TYPE>` syntax
   - **All Grants**: Grants expanded to all existing objects in the container
3. **Object Service**: Queries Snowflake to discover existing objects for expansion
4. **Builder Conversion**: Converts grant models to grant builders for statement generation

