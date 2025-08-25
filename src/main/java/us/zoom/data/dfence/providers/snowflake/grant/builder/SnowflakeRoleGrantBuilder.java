package us.zoom.data.dfence.providers.snowflake.grant.builder;

import lombok.AllArgsConstructor;
import lombok.Getter;
import us.zoom.data.dfence.exception.InvalidGrantModelForGrantBuilder;
import us.zoom.data.dfence.providers.snowflake.grant.builder.options.SnowflakeGrantBuilderOptions;
import us.zoom.data.dfence.providers.snowflake.models.GrantValidationDefinition;
import us.zoom.data.dfence.providers.snowflake.models.SnowflakeGrantModel;

import java.util.List;

@Getter
@AllArgsConstructor
public class SnowflakeRoleGrantBuilder extends SnowflakeGrantBuilder {

    private static final List<GrantValidationDefinition> validCombinations
            = List.of(new GrantValidationDefinition(List.of("USAGE"), List.of(SnowflakeObjectType.ROLE)));
    private SnowflakeGrantModel grant;
    private SnowflakeGrantBuilderOptions options;

    @Override
    public List<GrantValidationDefinition> getValidCombinations() {
        return validCombinations;
    }

    @Override
    public List<String> getGrantStatements() {
        if (!this.isValid()) {
            throw new InvalidGrantModelForGrantBuilder();
        }
        return List.of(String.format(
                "GRANT ROLE %s TO %s %s;",
                grant.getEscapedName(),
                grant.grantedTo(),
                grant.granteeName()));
    }

    @Override
    public List<String> getRevokeStatements() {
        if (!this.isValid()) {
            throw new InvalidGrantModelForGrantBuilder();
        }
        return List.of(String.format(
                "REVOKE ROLE %s FROM %s %s;",
                grant.getEscapedName(),
                grant.grantedTo(),
                grant.granteeName()));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (!(o instanceof SnowflakeRoleGrantBuilder that))
            return false;

        return getGrant() != null ? getGrant().equals(that.getGrant()) : that.getGrant() == null;
    }

    @Override
    public int hashCode() {
        return getGrant() != null ? getGrant().hashCode() : 0;
    }

    @Override
    public String toString() {
        return "SnowflakeRoleGrantBuilder{" + "grant=" + grant + '}';
    }
}
