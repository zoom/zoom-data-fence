package us.zoom.data.dfence.providers.snowflake.grant.builder;

import lombok.Getter;
import com.google.common.collect.ImmutableList;
import us.zoom.data.dfence.exception.InvalidGrantModelForGrantBuilder;
import us.zoom.data.dfence.providers.snowflake.grant.builder.options.SnowflakeGrantBuilderOptions;
import us.zoom.data.dfence.providers.snowflake.models.GrantValidationDefinition;
import us.zoom.data.dfence.providers.snowflake.models.SnowflakeGrantModel;

import java.util.ArrayList;
import java.util.List;

@Getter  
public class SnowflakeAccountPermissionGrantBuilder extends SnowflakeGrantBuilder {

    public static List<GrantValidationDefinition> validCombinations = List.of(new GrantValidationDefinition(
            ImmutableList.copyOf(new ArrayList<>() {{
                add("APPLY AUTHENTICATION POLICY");
                add("APPLY MASKING POLICY");
                add("APPLY MASKING POLICY");
                add("APPLY NETWORK POLICY");
                add("APPLY PACKAGES POLICY");
                add("APPLY PASSWORD POLICY");
                add("APPLY ROW ACCESS POLICY");
                add("APPLY SESSION POLICY");
                add("APPLY TAG");
                add("ATTACH POLICY");
                add("AUDIT");
                add("BIND SERVICE ENDPOINT");
                add("CREATE ACCOUNT");
                add("CREATE DATA EXCHANGE LISTING");
                add("CREATE DATABASE");
                add("CREATE INTEGRATION");
                add("CREATE NETWORK POLICY");
                add("CREATE NETWORK RULE");
                add("CREATE ROLE");
                add("CREATE SHARE");
                add("CREATE USER");
                add("CREATE WAREHOUSE");
                add("EXECUTE ALERT");
                add("EXECUTE TASK");
                add("IMPORT SHARE");
                add("MANAGE ACCOUNT SUPPORT CASES");
                add("MANAGE CALLER GRANTS");
                add("MANAGE GRANTS");
                add("MANAGE VISIBILITY");
                add("MANAGE USER SUPPORT CASES");
                add("MODIFY LOG LEVEL");
                add("MODIFY SESSION LOG LEVEL");
                add("MODIFY SESSION TRACE LEVEL");
                add("MODIFY TRACE LEVEL");
                add("MONITOR EXECUTION");
                add("MONITOR USAGE");
                add("MONITOR");
                add("OVERRIDE SHARE RESTRICTIONS");
                add("RESOLVE ALL");
                add("VIEW LINEAGE");
            }}), List.of(SnowflakeObjectType.ACCOUNT)));

    private final SnowflakeGrantModel grant;
    private final SnowflakeGrantBuilderOptions options;

    public SnowflakeAccountPermissionGrantBuilder(SnowflakeGrantModel grant, SnowflakeGrantBuilderOptions options) {
        this.options = options;
        // The account name parameter is not used in any of the grant statements and we
        // don't want to need to know it. We mask it out. This matters for key creation
        // with the getKey method.
        this.grant = new SnowflakeGrantModel(
                grant.privilege(),
                grant.grantedOn(),
                "",
                grant.grantedTo(),
                grant.granteeName(),
                grant.grantOption(),
                grant.future(),
                grant.all());
    }

    @Override
    public List<GrantValidationDefinition> getValidCombinations() {
        return validCombinations;
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
        return List.of(String.format(
                "GRANT %s ON ACCOUNT TO ROLE %s%s;",
                getGrant().privilege(),
                getGrant().granteeName(),
                withGrantOption));
    }

    @Override
    public List<String> getRevokeStatements() {
        if (!this.isValid()) {
            throw new InvalidGrantModelForGrantBuilder();
        }
        return List.of(String.format(
                "REVOKE %s ON ACCOUNT FROM ROLE %s;",
                getGrant().privilege(),
                getGrant().granteeName()));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (!(o instanceof SnowflakeAccountPermissionGrantBuilder that))
            return false;

        return getGrant().equals(that.getGrant());
    }

    @Override
    public int hashCode() {
        return getGrant().hashCode();
    }
}
