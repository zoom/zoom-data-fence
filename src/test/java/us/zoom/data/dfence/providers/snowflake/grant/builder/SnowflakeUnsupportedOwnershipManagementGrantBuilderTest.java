package us.zoom.data.dfence.providers.snowflake.grant.builder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;

import org.junit.jupiter.api.Test;

import us.zoom.data.dfence.exception.RbacDataError;
import us.zoom.data.dfence.providers.snowflake.grant.builder.options.SnowflakeGrantBuilderOptions;
import us.zoom.data.dfence.providers.snowflake.models.SnowflakeGrantModel;

class SnowflakeUnsupportedOwnershipManagementGrantBuilderTest {

  @Test
  void getGrantStatements() {
    SnowflakeGrantModel snowflakeGrantModel =
        new SnowflakeGrantModel(
            "OWNERSHIP",
            "STREAMLIT",
            "MY_DB.MY_SCHEMA.MY_STREAMLIT",
            "ROLE",
            "MY_ROLE",
            false,
            false,
            false);
    SnowflakeGrantBuilderOptions options = new SnowflakeGrantBuilderOptions();
    SnowflakeUnsupportedOwnershipManagementGrantBuilder grantBuilder =
        new SnowflakeUnsupportedOwnershipManagementGrantBuilder(snowflakeGrantModel, options);
    assertThrows(RbacDataError.class, () -> grantBuilder.getGrantStatements());
  }

  @Test
  void getRevokeStatements() {
    SnowflakeGrantModel snowflakeGrantModel =
        new SnowflakeGrantModel(
            "OWNERSHIP",
            "STREAMLIT",
            "MY_DB.MY_SCHEMA.MY_STREAMLIT",
            "ROLE",
            "MY_ROLE",
            false,
            false,
            false);
    SnowflakeGrantBuilderOptions options = new SnowflakeGrantBuilderOptions();
    SnowflakeUnsupportedOwnershipManagementGrantBuilder grantBuilder =
        new SnowflakeUnsupportedOwnershipManagementGrantBuilder(snowflakeGrantModel, options);
    List<String> expected = List.of();

    List<String> grantStatement = grantBuilder.getRevokeStatements();
    assertEquals(expected, grantStatement);
  }
}
