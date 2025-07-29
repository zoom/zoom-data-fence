package us.zoom.data.dfence.providers.snowflake.grant.builder;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.snowflake.client.jdbc.internal.google.common.collect.ImmutableList;
import us.zoom.data.dfence.exception.InvalidGrantModelForGrantBuilder;
import us.zoom.data.dfence.exception.RbacDataError;
import us.zoom.data.dfence.providers.snowflake.models.GrantValidationDefinition;
import us.zoom.data.dfence.providers.snowflake.models.SnowflakeGrantModel;

import java.util.ArrayList;
import java.util.List;


@Getter
@AllArgsConstructor
@Slf4j
public class SnowflakeUnsupportedOwnershipManagementGrantBuilder extends SnowflakeGrantBuilder {
    private static final List<GrantValidationDefinition> validCombinations = ImmutableList.copyOf(new ArrayList<>() {{
        add(new GrantValidationDefinition(
                List.of("OWNERSHIP"),
                List.of(SnowflakeObjectType.NOTEBOOK, SnowflakeObjectType.STREAMLIT)

        ));
    }});

    private final SnowflakeGrantModel grant;


    @Override
    public List<GrantValidationDefinition> getValidCombinations() {
        return validCombinations;
    }

    @Override
    public List<String> getGrantStatements() {
        if (!this.isValid()) {
            throw new InvalidGrantModelForGrantBuilder();
        }
        throw new RbacDataError(String.format("Ownership cannot be granted for a %s", grant.grantedOn()));
    }

    @Override
    public List<String> getRevokeStatements() {
        if (!this.isValid()) {
            throw new InvalidGrantModelForGrantBuilder();
        }
        log.warn(String.format(
                "It is not possible to revoke ownership on a notebook. We will not be able to drop ownership grants on %s %s.",
                grant.grantedOn(), grant.getEscapedName()));
        return List.of();
    }

    @Override
    public SnowflakeGrantModel getGrant() {
        return this.grant;
    }
}
