package us.zoom.data.dfence.providers.snowflake.models;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class SnowflakeGrantModelTest {

  @ParameterizedTest
  @CsvSource({
    "MOCK_DB.MOCK_SCHEMA.MOCK_TABLE,\"MOCK_DB\".\"MOCK_SCHEMA\".\"MOCK_TABLE\"",
    "MOCK_DB.MOCK_SCHEMA.\"MOCK_table\",\"MOCK_DB\".\"MOCK_SCHEMA\".\"MOCK_table\"",
    "MOCK_DB.\"mock_schema\".\"mock_table\",\"MOCK_DB\".\"mock_schema\".\"mock_table\"",
    "MOCK_DB.MOCK_SCHEMA,\"MOCK_DB\".\"MOCK_SCHEMA\"",
    "MOCK_DB,\"MOCK_DB\"",
    "mock_db,\"MOCK_DB\""
  })
  void getEscapedName(String name, String expected) {
    SnowflakeGrantModel snowflakeGrantModel =
        new SnowflakeGrantModel("SELECT", "TABLE", name, "ROLE", "MOCK_ROLE", false, false, false);
    String actual = snowflakeGrantModel.getEscapedName();
    assertEquals(expected, actual);
  }

  @ParameterizedTest
  @CsvSource({
    "MOCK_DB.MOCK_SCHEMA.MOCK_TABLE,MOCK_DB.MOCK_SCHEMA.MOCK_TABLE",
    "MOCK_DB.MOCK_SCHEMA.\"MOCK_table\",MOCK_DB.MOCK_SCHEMA.\"MOCK_table\"",
    "MOCK_DB.\"mock_schema\".\"mock_table\",MOCK_DB.\"mock_schema\".\"mock_table\"",
    "MOCK_DB.MOCK_SCHEMA,MOCK_DB.MOCK_SCHEMA",
    "MOCK_DB,MOCK_DB",
    "mock_db,MOCK_DB"
  })
  void getName(String name, String expected) {
    SnowflakeGrantModel snowflakeGrantModel =
        new SnowflakeGrantModel("SELECT", "TABLE", name, "ROLE", "MOCK_ROLE", false, false, false);
    String actual = snowflakeGrantModel.name();
    assertEquals(expected, actual);
  }

  @Test
  void grantModelProcedure() {
    SnowflakeGrantModel snowflakeGrantModel =
        new SnowflakeGrantModel(
            "USAGE",
            "PROCEDURE",
            "FOO.BAR.ZAR(VARCHAR, NUMBER)",
            "ROLE",
            "MOCK_ROLE",
            false,
            false,
            false);
  }
}
