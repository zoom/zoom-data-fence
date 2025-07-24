package us.zoom.security.dfence.playbook.model;

import jakarta.validation.constraints.NotEmpty;
import us.zoom.security.dfence.exception.RbacDataError;

import java.util.List;

public record PlaybookPrivilegeGrant(
        @NotEmpty String objectType,
        String objectName,
        String schemaName,
        String databaseName,
        @NotEmpty List<String> privileges,

        Boolean includeFuture,

        Boolean includeAll,

        Boolean enable) {
    public PlaybookPrivilegeGrant {
        if (privileges != null) {
            privileges = List.copyOf(privileges);
        } else {
            privileges = List.of();
        }
        if (includeFuture == null) {
            includeFuture = true;
        }
        if (includeAll == null) {
            includeAll = true;
        }
        if ("".equals(objectName)) {
            objectName = null;
        }
        if ("".equals(databaseName)) {
            databaseName = null;
        }
        if ("".equals(schemaName)) {
            schemaName = null;
        }
        if (enable == null) {
            enable = true;
        }
        if ("database".equalsIgnoreCase(objectType) && objectName != null && databaseName == null) {
            databaseName = objectName;
            objectName = null;
        }
        if ("schema".equalsIgnoreCase(objectType) && objectName != null && schemaName == null) {
            schemaName = objectName;
            objectName = null;
        }
        if (objectName == null && schemaName == null && databaseName == null && !"account".equalsIgnoreCase(objectType)) {
            throw new RbacDataError("One of objectName, schemaName or databaseName must be provided.");
        }
    }

    public PlaybookPrivilegeGrant(
            String objectType,
            String objectName,
            String schemaName,
            String databaseName,
            List<String> privileges,
            Boolean includeFuture,
            Boolean includeAll) {
        this(objectType, objectName, schemaName, databaseName, privileges, includeFuture, includeAll, null);
    }
}
