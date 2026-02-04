package us.zoom.data.dfence.providers.snowflake.grant.builder;

import io.vavr.NotImplementedError;
import us.zoom.data.dfence.exception.InvalidGrantModelForGrantBuilder;
import us.zoom.data.dfence.providers.snowflake.grant.builder.options.SnowflakeGrantBuilderOptions;
import us.zoom.data.dfence.providers.snowflake.models.ContainerGrantParts;
import us.zoom.data.dfence.providers.snowflake.models.SnowflakeGrantModel;

import java.util.List;

public class SnowflakeAllOwnershipGrantBuilder extends SnowflakeOwnershipGrantBuilder {
    public SnowflakeAllOwnershipGrantBuilder(
            SnowflakeGrantModel grant, SnowflakeGrantBuilderOptions options) {
        super(grant, options);
    }

    @Override
    public Boolean isValid() {
        return this.getValidCombinations().stream().anyMatch(x -> x.validateAll(getGrant()));
    }

    @Override
    public List<String> getGrantStatements() {
        if (!this.isValid()) {
            throw new InvalidGrantModelForGrantBuilder();
        }
        ContainerGrantParts futureGrantObjectNameParts
                = GrantObjectNameParser.futureGrantObjectNameParts(getGrant().name());
        return List.of(String.format(
                "GRANT OWNERSHIP ON ALL %s IN %s %s TO ROLE %s COPY CURRENT GRANTS;",
                futureGrantObjectNameParts.objectType().getSqlQueryObjectTypePlural(),
                futureGrantObjectNameParts.containerObjectType().getSqlQueryObjectType(),
                futureGrantObjectNameParts.objectName(),
                getGrant().granteeName()));
    }

    @Override
    public List<String> getRevokeStatements() {
        throw new NotImplementedError("Cannot revoke all grants.");
    }

    @Override
    public String toString() {
        return "SnowflakeAllOwnershipGrantBuilder{" + "grant=" + super.getGrant() + '}';
    }
}
