package us.zoom.data.dfence.providers.snowflake.grant.builder;

import io.vavr.NotImplementedError;
import lombok.extern.slf4j.Slf4j;
import us.zoom.data.dfence.exception.InvalidGrantModelForGrantBuilder;
import us.zoom.data.dfence.providers.snowflake.models.ContainerGrantParts;
import us.zoom.data.dfence.providers.snowflake.models.SnowflakeGrantModel;

import java.util.List;

@Slf4j
public class SnowflakeAllCorePermissionGrantBuilder extends SnowflakeCorePermissionGrantBuilder {

    public SnowflakeAllCorePermissionGrantBuilder(SnowflakeGrantModel grant) {
        super(grant);
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
        String withGrantOption = "";
        if (getGrant().grantOption()) {
            withGrantOption = " with grant option";
        }
        ContainerGrantParts futureGrantObjectNameParts
                = GrantObjectNameParser.futureGrantObjectNameParts(getGrant().name());
        return List.of(String.format(
                "GRANT %s ON ALL %s IN %s %s TO ROLE %s%s;",
                transformPrivilege(getGrant().privilege()),
                futureGrantObjectNameParts.objectType().getObjectTypePlural(),
                futureGrantObjectNameParts.containerObjectType().getObjectType(),
                futureGrantObjectNameParts.objectName(),
                getGrant().granteeName(),
                withGrantOption));
    }

    @Override
    public List<String> getRevokeStatements() {
        throw new NotImplementedError("getRevokeStatements should not be called on all.");
    }

    @Override
    public boolean equals(Object o) {
        return super.equals(o);
    }

    @Override
    public int hashCode() {
        return super.hashCode();
    }

    @Override
    public String toString() {
        return "SnowflakeAllPermissionGrantBuilder{" + "grant=" + super.getGrant() + '}';
    }
}
