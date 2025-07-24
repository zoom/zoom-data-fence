package us.zoom.security.dfence.providers.snowflake.grant.builder;

import lombok.Getter;
import us.zoom.security.dfence.exception.InvalidGrantModelForGrantBuilder;
import us.zoom.security.dfence.providers.snowflake.models.GrantValidationDefinition;
import us.zoom.security.dfence.providers.snowflake.models.SnowflakeGrantModel;

import java.util.List;
import java.util.Objects;

public class SnowflakeApplicationRoleGrantBuilder extends SnowflakeGrantBuilder {

    private static final List<GrantValidationDefinition> validCombinations
            = List.of(new GrantValidationDefinition(List.of("USAGE"), List.of(SnowflakeObjectType.APPLICATION_ROLE)));

    public SnowflakeApplicationRoleGrantBuilder(SnowflakeGrantModel grant) {
        this.grant = grant;
    }

    @Override
    public List<GrantValidationDefinition> getValidCombinations() {
        return validCombinations;
    }

    @Getter
    private SnowflakeGrantModel grant;

    @Override
    public List<String> getGrantStatements() {
        if (!this.isValid()) {
            throw new InvalidGrantModelForGrantBuilder();
        }
        return List.of(String.format(
                "GRANT APPLICATION ROLE %s TO %s %s;",
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
                "REVOKE APPLICATION ROLE %s FROM %s %s;",
                grant.getEscapedName(),
                grant.grantedTo(),
                grant.granteeName()));
    }


    @Override
    public final boolean equals(Object o) {
        if (!(o instanceof SnowflakeApplicationRoleGrantBuilder that))
            return false;
        return Objects.equals(getGrant(), that.getGrant());
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + Objects.hashCode(getGrant());
        return result;
    }

    @Override
    public String toString() {
        return "SnowflakeApplicationRoleGrantBuilder{" + "grant=" + grant + '}';
    }
}
