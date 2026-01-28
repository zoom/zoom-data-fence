package us.zoom.data.dfence.providers.snowflake.grant.diff.models;

import java.util.List;
import us.zoom.data.dfence.playbook.model.PlaybookPrivilegeGrant;
import us.zoom.data.dfence.providers.snowflake.grant.builder.options.SnowflakeGrantBuilderOptions;

public record AllDesiredGrantsCreationDataForRole(
    List<PlaybookPrivilegeGrant> privilegeGrants,
    String roleName,
    SnowflakeGrantBuilderOptions options) {}
