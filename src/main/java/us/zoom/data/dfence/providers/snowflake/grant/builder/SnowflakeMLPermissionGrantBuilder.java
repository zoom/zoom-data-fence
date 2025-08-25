package us.zoom.data.dfence.providers.snowflake.grant.builder;

import com.google.common.collect.ImmutableList;
import lombok.AllArgsConstructor;
import lombok.Getter;
import us.zoom.data.dfence.exception.InvalidGrantModelForGrantBuilder;
import us.zoom.data.dfence.exception.RbacDataError;
import us.zoom.data.dfence.providers.snowflake.grant.builder.options.SnowflakeGrantBuilderOptions;
import us.zoom.data.dfence.providers.snowflake.models.GrantValidationDefinition;
import us.zoom.data.dfence.providers.snowflake.models.SnowflakeGrantModel;

import java.util.ArrayList;
import java.util.List;

@Getter
@AllArgsConstructor
public class SnowflakeMLPermissionGrantBuilder extends SnowflakeGrantBuilder {
    private static final List<GrantValidationDefinition> validCombinations = ImmutableList.copyOf(new ArrayList<>() {{
        add(new GrantValidationDefinition(
                ImmutableList.copyOf(new ArrayList<>() {{
                    add("CREATE ANOMALY_DETECTION");
                    add("CREATE FORECAST");
                }}), List.of(SnowflakeObjectType.SCHEMA)));
    }});

    private SnowflakeGrantModel grant;
    private SnowflakeGrantBuilderOptions options;

    public static String transformPrivilege(String permission) {
        if (!permission.startsWith("CREATE ")) {
            throw new RbacDataError("Wrong permission format.");
        }
        String permissionTarget = permission.replace("CREATE ", "");
        return "CREATE SNOWFLAKE.ML." + permissionTarget;
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
        if (grant.grantOption()) {
            withGrantOption = " WITH GRANT OPTION";
        }
        return List.of(String.format(
                "GRANT %s ON %s %s TO ROLE %s%s;",
                transformPrivilege(grant.privilege()),
                SnowflakeObjectType.valueOf(grant.grantedOn()).getObjectType(),
                grant.getEscapedName(),
                grant.granteeName(),
                withGrantOption));
    }

    @Override
    public List<String> getRevokeStatements() {
        if (!this.isValid()) {
            throw new InvalidGrantModelForGrantBuilder();
        }
        return List.of(String.format(
                "REVOKE %s ON %s %s FROM ROLE %s;",
                transformPrivilege(grant.privilege()),
                SnowflakeObjectType.valueOf(grant.grantedOn()).getObjectType(),
                grant.getEscapedName(),
                grant.granteeName()));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (!(o instanceof SnowflakeMLPermissionGrantBuilder that))
            return false;

        return getGrant() != null ? getGrant().equals(that.getGrant()) : that.getGrant() == null;
    }

    @Override
    public int hashCode() {
        return getGrant() != null ? getGrant().hashCode() : 0;
    }

    @Override
    public String toString() {
        return "SnowflakePermissionGrantBuilder{" + "grant=" + grant + '}';
    }
}
