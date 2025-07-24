package us.zoom.security.dfence.providers.snowflake.grant.builder;

import us.zoom.security.dfence.exception.InvalidGrantModelForGrantBuilder;
import us.zoom.security.dfence.providers.snowflake.models.ContainerGrantParts;
import us.zoom.security.dfence.providers.snowflake.models.SnowflakeGrantModel;

import java.util.List;

public class SnowflakeFutureOwnershipGrantBuilder extends SnowflakeOwnershipGrantBuilder {
    public SnowflakeFutureOwnershipGrantBuilder(
            SnowflakeGrantModel grant) {
        super(grant);
    }

    @Override
    public Boolean isValid() {
        return this.getValidCombinations().stream().anyMatch(x -> x.validateFuture(getGrant()));
    }

    @Override
    public List<String> getGrantStatements() {
        if (!this.isValid()) {
            throw new InvalidGrantModelForGrantBuilder();
        }
        ContainerGrantParts futureGrantObjectNameParts
                = GrantObjectNameParser.futureGrantObjectNameParts(getGrant().name());
        return List.of(String.format(
                "GRANT OWNERSHIP ON FUTURE %s IN %s %s TO ROLE %s COPY CURRENT GRANTS;",
                futureGrantObjectNameParts.objectType().getObjectTypePlural(),
                futureGrantObjectNameParts.containerObjectType().getObjectType(),
                futureGrantObjectNameParts.objectName(),
                getGrant().granteeName()));
    }

    @Override
    public List<String> getRevokeStatements() {
        if (!this.isValid()) {
            throw new InvalidGrantModelForGrantBuilder();
        }
        ContainerGrantParts futureGrantObjectNameParts
                = GrantObjectNameParser.futureGrantObjectNameParts(getGrant().name());
        return List.of(String.format(
                "REVOKE OWNERSHIP ON FUTURE %s IN %s %s FROM ROLE %s;",
                futureGrantObjectNameParts.objectType().getObjectTypePlural(),
                futureGrantObjectNameParts.containerObjectType().getObjectType(),
                futureGrantObjectNameParts.objectName(),
                getGrant().granteeName()));
    }
}
