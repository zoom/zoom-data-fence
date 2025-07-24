package us.zoom.security.dfence.playbook.model;

import java.util.List;

public record PlaybookRoleModel(
        String name, List<PlaybookPrivilegeGrant> grants,

        Boolean create, Boolean revokeOtherGrants, Boolean enable,

        String roleOwner) {
    public PlaybookRoleModel {
        if (grants != null) {
            grants = List.copyOf(grants);
        } else {
            grants = List.of();
        }
        if (create == null) {
            create = true;
        }
        if (enable == null) {
            enable = true;
        }
        if (revokeOtherGrants == null) {
            revokeOtherGrants = true;
        }
    }

    public PlaybookRoleModel(String name, List<PlaybookPrivilegeGrant> grants) {
        this(name, grants, null, null, null, null);
    }
}
