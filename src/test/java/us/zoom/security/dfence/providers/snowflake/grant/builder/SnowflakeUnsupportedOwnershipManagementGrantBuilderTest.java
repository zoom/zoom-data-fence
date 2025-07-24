package us.zoom.security.dfence.providers.snowflake.grant.builder;

import org.junit.jupiter.api.Test;
import us.zoom.security.dfence.exception.RbacDataError;
import us.zoom.security.dfence.providers.snowflake.models.SnowflakeGrantModel;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SnowflakeUnsupportedOwnershipManagementGrantBuilderTest {

    @Test
    void getGrantStatements() {
        SnowflakeGrantModel snowflakeGrantModel = new SnowflakeGrantModel(
                "OWNERSHIP",
                "STREAMLIT",
                "MY_DB.MY_SCHEMA.MY_STREAMLIT",
                "ROLE",
                "MY_ROLE",
                false,
                false,
                false);
        SnowflakeUnsupportedOwnershipManagementGrantBuilder grantBuilder
                = new SnowflakeUnsupportedOwnershipManagementGrantBuilder(snowflakeGrantModel);
        assertThrows(RbacDataError.class, () -> grantBuilder.getGrantStatements());
    }

    @Test
    void getRevokeStatements() {
        SnowflakeGrantModel snowflakeGrantModel = new SnowflakeGrantModel(
                "OWNERSHIP",
                "STREAMLIT",
                "MY_DB.MY_SCHEMA.MY_STREAMLIT",
                "ROLE",
                "MY_ROLE",
                false,
                false,
                false);
        SnowflakeUnsupportedOwnershipManagementGrantBuilder grantBuilder
                = new SnowflakeUnsupportedOwnershipManagementGrantBuilder(snowflakeGrantModel);
        List<String> expected = List.of("DROP STREAMLIT \"MY_DB\".\"MY_SCHEMA\".\"MY_STREAMLIT\";");

        List<String> grantStatement = grantBuilder.getRevokeStatements();
        assertEquals(expected, grantStatement);
    }
}