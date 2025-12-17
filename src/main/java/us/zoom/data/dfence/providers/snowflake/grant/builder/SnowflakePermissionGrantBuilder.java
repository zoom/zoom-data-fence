package us.zoom.data.dfence.providers.snowflake.grant.builder;

import com.google.common.collect.ImmutableList;
import lombok.AllArgsConstructor;
import lombok.Getter;
import us.zoom.data.dfence.exception.InvalidGrantModelForGrantBuilder;
import us.zoom.data.dfence.providers.snowflake.grant.builder.options.SnowflakeGrantBuilderOptions;
import us.zoom.data.dfence.providers.snowflake.models.GrantValidationDefinition;
import us.zoom.data.dfence.providers.snowflake.models.SnowflakeGrantModel;

import java.util.ArrayList;
import java.util.List;

@Getter
@AllArgsConstructor
public class SnowflakePermissionGrantBuilder extends SnowflakeGrantBuilder {
    private static final List<GrantValidationDefinition> validCombinations = ImmutableList.copyOf(new ArrayList<>() {{
        add(new GrantValidationDefinition(
                List.of(
                        "SELECT",
                        "INSERT",
                        "UPDATE",
                        "DELETE",
                        "TRUNCATE",
                        "REFERENCES",
                        "REBUILD",
                        "EVOLVE SCHEMA",
                        "APPLYBUDGET"),
                List.of(
                        SnowflakeObjectType.TABLE,
                        SnowflakeObjectType.EXTERNAL_TABLE,
                        SnowflakeObjectType.ICEBERG_TABLE)));
        add(new GrantValidationDefinition(List.of("SELECT", "INSERT"), List.of(SnowflakeObjectType.EVENT_TABLE)));
        add(new GrantValidationDefinition(
                List.of(
                        "SELECT",
                        "REFERENCES",
                        "DELETE",
                        "INSERT",
                        "TRUNCATE",
                        "UPDATE",
                        "REBUILD",
                        "EVOLVE SCHEMA",
                        "APPLYBUDGET"), List.of(SnowflakeObjectType.MATERIALIZED_VIEW, SnowflakeObjectType.VIEW)));
        add(new GrantValidationDefinition(
                List.of("USAGE"), List.of(
                SnowflakeObjectType.SEQUENCE,
                SnowflakeObjectType.FUNCTION,
                SnowflakeObjectType.PROCEDURE,
                SnowflakeObjectType.FILE_FORMAT)));
        // We are allowing usage, read and write here. However, external stages only allow usage and internal stages
        // only allow read and write. Snowflake does not distinguish stages types based on object name.
        add(new GrantValidationDefinition(List.of("USAGE", "READ", "WRITE"), List.of(SnowflakeObjectType.STAGE)));
        add(new GrantValidationDefinition(List.of("MONITOR", "OPERATE"), List.of(SnowflakeObjectType.PIPE)));
        add(new GrantValidationDefinition(List.of("SELECT"), List.of(SnowflakeObjectType.STREAM)));
        add(new GrantValidationDefinition(List.of("MONITOR", "OPERATE"), List.of(SnowflakeObjectType.TASK)));
        add(new GrantValidationDefinition(
                List.of("APPLY"), List.of(
                SnowflakeObjectType.MASKING_POLICY,
                SnowflakeObjectType.PASSWORD_POLICY,
                SnowflakeObjectType.ROW_ACCESS_POLICY,
                SnowflakeObjectType.SESSION_POLICY,
                SnowflakeObjectType.TAG)));
        add(new GrantValidationDefinition(List.of("OPERATE"), List.of(SnowflakeObjectType.ALERT)));
        add(new GrantValidationDefinition(List.of("USAGE"), List.of(SnowflakeObjectType.SECRET)));
        add(new GrantValidationDefinition(
                ImmutableList.copyOf(new ArrayList<>() {{
                    add("ADD SEARCH OPTIMIZATION");
                    add("APPLYBUDGET");
                    add("CREATE AGGREGATION POLICY");
                    add("CREATE AGENT");
                    add("CREATE ALERT");
                    add("CREATE AUTHENTICATION POLICY");
                    add("CREATE CLASS");
                    add("CREATE CONTACT");
                    add("CREATE DYNAMIC TABLE");
                    add("CREATE EVENT TABLE");
                    add("CREATE EXTERNAL TABLE");
                    add("CREATE FILE FORMAT");
                    add("CREATE FUNCTION");
                    add("CREATE GIT REPOSITORY");
                    add("CREATE ICEBERG TABLE");
                    add("CREATE IMAGE REPOSITORY");
                    add("CREATE INSTANCE");
                    add("CREATE JOIN POLICY");
                    add("CREATE MASKING POLICY");
                    add("CREATE MATERIALIZED VIEW");
                    add("CREATE MODEL");
                    add("CREATE MODEL MONITOR");
                    add("CREATE NETWORK RULE");
                    add("CREATE NOTEBOOK");
                    add("CREATE PACKAGES POLICY");
                    add("CREATE CORTEX SEARCH SERVICE");
                    add("CREATE DATA METRIC FUNCTION");
                    add("CREATE DATASET");
                    add("CREATE PASSWORD POLICY");
                    add("CREATE PIPE");
                    add("CREATE PRIVACY POLICY");
                    add("CREATE PROCEDURE");
                    add("CREATE PROJECTION POLICY");
                    add("CREATE RESOURCE GROUP");
                    add("CREATE ROW ACCESS POLICY");
                    add("CREATE SECRET");
                    add("CREATE SEQUENCE");
                    add("CREATE SEMANTIC VIEW");
                    add("CREATE SERVICE CLASS");
                    add("CREATE SERVICE");
                    add("CREATE SESSION POLICY");
                    add("CREATE SNAPSHOT");
                    add("CREATE SNOWFLAKE.CORE.BUDGET");
                    add("CREATE SNOWFLAKE.ML.ANOMALY_DETECTION");
                    add("CREATE SNOWFLAKE.ML.FORECAST");
                    add("CREATE STAGE");
                    add("CREATE STREAM");
                    add("CREATE STREAMLIT");
                    add("CREATE TABLE");
                    add("CREATE TAG");
                    add("CREATE TASK");
                    add("CREATE TEMPORARY TABLE");
                    add("CREATE VIEW");
                    add("EXECUTE AUTO CLASSIFICATION");
                    add("MODIFY");
                    add("MONITOR");
                    add("USAGE");
                }}), List.of(SnowflakeObjectType.SCHEMA)));
        add(new GrantValidationDefinition(List.of("MONITOR"), List.of(SnowflakeObjectType.USER)));
        add(new GrantValidationDefinition(List.of("MONITOR", "MODIFY"), List.of(SnowflakeObjectType.RESOURCE_MONITOR)));
        add(new GrantValidationDefinition(
                List.of("MONITOR", "MODIFY", "USAGE", "OPERATE"),
                List.of(SnowflakeObjectType.WAREHOUSE)));
        add(new GrantValidationDefinition(
                List.of(
                        "CREATE DATABASE ROLE",
                        "CREATE SCHEMA",
                        "IMPORTED PRIVILEGES",
                        "USAGE",
                        "MODIFY",
                        "MONITOR",
                        "REFERENCE_USAGE"), List.of(SnowflakeObjectType.DATABASE)));
        add(new GrantValidationDefinition(List.of("USAGE", "USE_ANY_ROLE"), List.of(SnowflakeObjectType.INTEGRATION)));
        add(new GrantValidationDefinition(List.of("USAGE"), List.of(SnowflakeObjectType.STREAMLIT)));
        add(new GrantValidationDefinition(List.of("INSTANTIATE", "USAGE"), List.of(SnowflakeObjectType.CLASS)));
        add(new GrantValidationDefinition(List.of("USAGE"), List.of(SnowflakeObjectType.INSTANCE)));
        add(new GrantValidationDefinition(List.of("USAGE"), List.of(SnowflakeObjectType.VOLUME)));
        add(new GrantValidationDefinition(List.of("READ"), List.of(SnowflakeObjectType.TAG)));
        add(new GrantValidationDefinition(
                List.of("MODIFY", "MONITOR", "OPERATE", "USAGE"),
                List.of(SnowflakeObjectType.COMPUTE_POOL)));
        add(new GrantValidationDefinition(List.of("READ", "WRITE"), List.of(SnowflakeObjectType.IMAGE_REPOSITORY)));
        add(new GrantValidationDefinition(List.of("SELECT", "REFERENCES"), List.of(SnowflakeObjectType.SEMANTIC_VIEW)));
        add(new GrantValidationDefinition(List.of("USAGE", "MODIFY", "MONITOR"), List.of(SnowflakeObjectType.CORTEX_AGENT)));
    }});

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
        String withGrantOption = "";
        if (grant.grantOption()) {
            withGrantOption = " WITH GRANT OPTION";
        }
        return List.of(String.format(
                "GRANT %s ON %s %s TO ROLE %s%s;",
                grant.privilege(),
                SnowflakeObjectType.fromString(grant.grantedOn()).getObjectType(),
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
                grant.privilege(),
                SnowflakeObjectType.fromString(grant.grantedOn()).getObjectType(),
                grant.getEscapedName(),
                grant.granteeName()));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (!(o instanceof SnowflakePermissionGrantBuilder that))
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
