package us.zoom.data.dfence.providers.snowflake.grant.builder;

import us.zoom.data.dfence.exception.InvalidGrantModelForGrantBuilder;
import us.zoom.data.dfence.providers.snowflake.grant.builder.options.SnowflakeGrantBuilderOptions;
import us.zoom.data.dfence.providers.snowflake.models.ContainerGrantParts;
import us.zoom.data.dfence.providers.snowflake.models.SnowflakeGrantModel;

import java.util.List;

public class SnowflakeFuturePermissionGrantBuilder extends SnowflakePermissionGrantBuilder {
    public SnowflakeFuturePermissionGrantBuilder(
            SnowflakeGrantModel grant, SnowflakeGrantBuilderOptions options) {
        super(grant, options);
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
        String withGrantOption = "";
        if (getGrant().grantOption()) {
            withGrantOption = " with grant option";
        }
        ContainerGrantParts futureGrantObjectNameParts
                = GrantObjectNameParser.futureGrantObjectNameParts(getGrant().name());
        return List.of(String.format(
                "GRANT %s ON FUTURE %s IN %s %s TO ROLE %s%s;",
                getGrant().privilege(),
                futureGrantObjectNameParts.objectType().getSqlQueryObjectTypePlural(),
                futureGrantObjectNameParts.containerObjectType().getSqlQueryObjectType(),
                futureGrantObjectNameParts.objectName(),
                getGrant().granteeName(),
                withGrantOption));
    }

    @Override
    public List<String> getRevokeStatements() {
        if (!this.isValid()) {
            throw new InvalidGrantModelForGrantBuilder();
        }
        ContainerGrantParts futureGrantObjectNameParts
                = GrantObjectNameParser.futureGrantObjectNameParts(getGrant().name());
        return List.of(String.format(
                "REVOKE %s ON FUTURE %s IN %s %s FROM ROLE %s;",
                getGrant().privilege(),
                futureGrantObjectNameParts.objectType().getSqlQueryObjectTypePlural(),
                futureGrantObjectNameParts.containerObjectType().getSqlQueryObjectType(),
                futureGrantObjectNameParts.objectName(),
                getGrant().granteeName()));

    }
}
