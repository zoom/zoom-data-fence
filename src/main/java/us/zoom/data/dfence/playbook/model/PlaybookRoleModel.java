package us.zoom.data.dfence.playbook.model;

import us.zoom.data.dfence.providers.snowflake.grant.builder.options.UnsupportedRevokeBehavior;

import java.util.List;

public record PlaybookRoleModel(
        String name, List<PlaybookPrivilegeGrant> grants,

        Boolean create, Boolean revokeOtherGrants, Boolean enable,

        String roleOwner, UnsupportedRevokeBehavior unsupportedRevokeBehavior) {
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
        if (unsupportedRevokeBehavior == null) {
            unsupportedRevokeBehavior = UnsupportedRevokeBehavior.IGNORE;
        }
    }

    public PlaybookRoleModel(String name, List<PlaybookPrivilegeGrant> grants) {
        this(name, grants, null, null, null, null, null);
    }
}
