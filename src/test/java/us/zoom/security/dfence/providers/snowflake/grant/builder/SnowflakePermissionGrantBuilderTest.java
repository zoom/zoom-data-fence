package us.zoom.security.dfence.providers.snowflake.grant.builder;

import org.junit.jupiter.api.BeforeEach;
import us.zoom.security.dfence.providers.snowflake.models.SnowflakeGrantModel;

class SnowflakePermissionGrantBuilderTest {

    SnowflakePermissionGrantBuilder permissionGrantBuilder;

    @BeforeEach
    void setUp() {
        permissionGrantBuilder = new SnowflakePermissionGrantBuilder(new SnowflakeGrantModel(
                "SELECT",
                "TABLE",
                "MY_TABLE",
                "ROLE",
                "MOCK_ROLE",
                false,
                false,
                false));
    }
}