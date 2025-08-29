package us.zoom.data.dfence.providers.snowflake.models;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import us.zoom.data.dfence.providers.snowflake.grant.builder.SnowflakeObjectType;

class GrantValidationDefinitionTest {

  @Test
  void validate() {
    GrantValidationDefinition grantValidationDefinition =
        new GrantValidationDefinition(
            List.of("MOCK PRIVILEGE", "MOCK OTHER PRIVILEGE"),
            List.of(SnowflakeObjectType.ACCOUNT));
    SnowflakeGrantModel snowflakeGrantModel =
        new SnowflakeGrantModel(
            "MOCK PRIVILEGE", "ACCOUNT", "", "ROLE", "MOCK_GRANTEE", false, false, false);
    assertTrue(grantValidationDefinition.validate(snowflakeGrantModel));
  }

  @Test
  void validateFuture() {
    GrantValidationDefinition grantValidationDefinition =
        new GrantValidationDefinition(
            List.of("MOCK PRIVILEGE", "MOCK OTHER PRIVILEGE"),
            List.of(SnowflakeObjectType.DATABASE));
    SnowflakeGrantModel snowflakeGrantModel =
        new SnowflakeGrantModel(
            "MOCK PRIVILEGE",
            "DATABASE",
            "MOCK_DATABASE.<TABLES>",
            "ROLE",
            "MOCK_GRANTEE",
            false,
            true,
            false);
    assertTrue(grantValidationDefinition.validateFuture(snowflakeGrantModel));
  }
}
