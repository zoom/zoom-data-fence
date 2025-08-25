package us.zoom.data.dfence.providers.snowflake.grant.builder;

import org.junit.jupiter.api.BeforeEach;
import us.zoom.data.dfence.providers.snowflake.grant.builder.options.SnowflakeGrantBuilderOptions;
import us.zoom.data.dfence.providers.snowflake.models.SnowflakeGrantModel;

class SnowflakePermissionGrantBuilderTest {

    SnowflakePermissionGrantBuilder permissionGrantBuilder;

    @BeforeEach
    void setUp() {
        SnowflakeGrantBuilderOptions options = new SnowflakeGrantBuilderOptions();
        permissionGrantBuilder = new SnowflakePermissionGrantBuilder(new SnowflakeGrantModel(
                "SELECT",
                "TABLE",
                "MY_TABLE",
                "ROLE",
                "MOCK_ROLE",
                false,
                false,
                false), options);
    }
}