package us.zoom.security.dfence.providers.snowflake.models;

import us.zoom.security.dfence.providers.snowflake.grant.builder.SnowflakeObjectType;

import java.util.List;

public record GrantValidationDefinition(
        List<String> privileges, List<SnowflakeObjectType> objectTypes) {
    public Boolean validate(SnowflakeGrantModel grant) {
        return !grant.future() && !grant.all() && privileges.contains(grant.privilege()) && objectTypes.stream()
                .anyMatch(ot -> ot.name().equals(grant.grantedOn()));
    }

    public Boolean validateFuture(SnowflakeGrantModel grant) {
        return !grant.all() && grant.future() && privileges.contains(grant.privilege()) && objectTypes.stream()
                .anyMatch(ot -> ot.name().equals(grant.grantedOn()));
    }

    public Boolean validateAll(SnowflakeGrantModel grant) {
        return !grant.future() && grant.all() && privileges.contains(grant.privilege()) && objectTypes.stream()
                .anyMatch(ot -> ot.name().equals(grant.grantedOn()));
    }
}
