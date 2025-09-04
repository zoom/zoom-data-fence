package us.zoom.data.dfence.providers.snowflake.grant.builder;

import com.google.common.collect.ImmutableList;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;
import us.zoom.data.dfence.exception.InvalidGrantModelForGrantBuilder;
import us.zoom.data.dfence.providers.snowflake.grant.builder.options.SnowflakeGrantBuilderOptions;
import us.zoom.data.dfence.providers.snowflake.models.GrantValidationDefinition;
import us.zoom.data.dfence.providers.snowflake.models.SnowflakeGrantModel;

import java.util.ArrayList;
import java.util.List;

@Getter
@AllArgsConstructor
@ToString
public class SnowflakeOwnershipGrantBuilder extends SnowflakeGrantBuilder {
    private static final List<GrantValidationDefinition> validCombinations = ImmutableList.copyOf(new ArrayList<>() {{
        add(new GrantValidationDefinition(
                List.of("OWNERSHIP"), List.of(
                SnowflakeObjectType.ALERT,
                SnowflakeObjectType.COMPUTE_POOL,
                SnowflakeObjectType.DATABASE,
                SnowflakeObjectType.DATABASE_ROLE,
                SnowflakeObjectType.DIRECTORY_TABLE,
                SnowflakeObjectType.EVENT_TABLE,
                SnowflakeObjectType.EXTERNAL_TABLE,
                SnowflakeObjectType.FILE_FORMAT,
                SnowflakeObjectType.FUNCTION,
                SnowflakeObjectType.ICEBERG_TABLE,
                SnowflakeObjectType.INSTANCE,
                SnowflakeObjectType.INTEGRATION,
                SnowflakeObjectType.MASKING_POLICY,
                SnowflakeObjectType.MATERIALIZED_VIEW,
                SnowflakeObjectType.NETWORK_POLICY,
                SnowflakeObjectType.NETWORK_RULE,
                SnowflakeObjectType.PASSWORD_POLICY,
                SnowflakeObjectType.PIPE,
                SnowflakeObjectType.PROCEDURE,
                SnowflakeObjectType.ROLE,
                SnowflakeObjectType.ROW_ACCESS_POLICY,
                SnowflakeObjectType.SCHEMA,
                SnowflakeObjectType.SECRET,
                SnowflakeObjectType.SEMANTIC_VIEW,
                SnowflakeObjectType.SEQUENCE,
                SnowflakeObjectType.SESSION_POLICY,
                SnowflakeObjectType.STAGE,
                SnowflakeObjectType.STREAM,
                SnowflakeObjectType.TABLE,
                SnowflakeObjectType.TAG,
                SnowflakeObjectType.TASK,
                SnowflakeObjectType.USER,
                SnowflakeObjectType.VIEW,
                SnowflakeObjectType.VOLUME,
                SnowflakeObjectType.WAREHOUSE)

        ));
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
        return List.of(String.format(
                "GRANT OWNERSHIP ON %s %s TO ROLE %s COPY CURRENT GRANTS;",
                SnowflakeObjectType.valueOf(grant.grantedOn()).getObjectType(),
                this.grant.getEscapedName(),
                this.grant.granteeName()));
    }

    @Override
    public List<String> getRevokeStatements() {
        if (!this.isValid()) {
            throw new InvalidGrantModelForGrantBuilder();
        }
        return List.of(String.format(
                "GRANT OWNERSHIP ON %s %s TO ROLE SECURITYADMIN COPY CURRENT GRANTS;",
                SnowflakeObjectType.valueOf(grant.grantedOn()).getObjectType(),
                this.grant.getEscapedName()));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (!(o instanceof SnowflakeOwnershipGrantBuilder that))
            return false;

        return getGrant() != null ? getGrant().equals(that.getGrant()) : that.getGrant() == null;
    }

    @Override
    public int hashCode() {
        return getGrant() != null ? getGrant().hashCode() : 0;
    }
}
