package us.zoom.data.dfence.providers.snowflake.revoke.companions;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;
import us.zoom.data.dfence.providers.snowflake.grant.builder.SnowflakeObjectType;
import us.zoom.data.dfence.providers.snowflake.models.SnowflakeGrantModel;
import us.zoom.data.dfence.providers.snowflake.revoke.models.SnowflakeGrant;
import us.zoom.data.dfence.providers.snowflake.revoke.models.SnowflakeGrantType;

class SnowflakeGrantCompanionTest {

  @Test
  void from_shouldConvertStandardGrant_whenFutureAndAllAreFalse() {
    // Given
    SnowflakeGrantModel model =
        new SnowflakeGrantModel(
            "SELECT", "TABLE", "DB.SCHEMA.TABLE", "ROLE", "USER", false, false, false);

    // When
    SnowflakeGrant result = SnowflakeGrantCompanion.from(model);

    // Then
    assertEquals(SnowflakeGrantType.STANDARD, result.grantType());
    assertEquals("SELECT", result.privilege().value());
  }

  @Test
  void from_shouldConvertFutureGrant_whenFutureIsTrueAndAllIsFalse() {
    // Given
    SnowflakeGrantModel model =
        new SnowflakeGrantModel("SELECT", "TABLE", "DB.SCHEMA", "ROLE", "USER", false, true, false);

    // When
    SnowflakeGrant result = SnowflakeGrantCompanion.from(model);

    // Then
    assertEquals(SnowflakeGrantType.FUTURE, result.grantType());
  }

  @Test
  void from_shouldConvertAllGrant_whenAllIsTrueAndFutureIsFalse() {
    // Given
    SnowflakeGrantModel model =
        new SnowflakeGrantModel("SELECT", "TABLE", "DB.SCHEMA", "ROLE", "USER", false, false, true);

    // When
    SnowflakeGrant result = SnowflakeGrantCompanion.from(model);

    // Then
    assertEquals(SnowflakeGrantType.ALL, result.grantType());
  }

  @Test
  void from_shouldThrow_whenBothFutureAndAllAreTrue() {
    // Given: When both are true, it throws an error (invalid state)
    SnowflakeGrantModel model =
        new SnowflakeGrantModel("SELECT", "TABLE", "DB.SCHEMA", "ROLE", "USER", false, true, true);

    // When/Then: Should throw when both future and all are true
    assertThrows(Exception.class, () -> SnowflakeGrantCompanion.from(model));
  }

  @Test
  void from_shouldThrow_whenObjectTypeIsInvalid() {
    // Given
    SnowflakeGrantModel model =
        new SnowflakeGrantModel(
            "SELECT",
            "INVALID_OBJECT_TYPE",
            "DB.SCHEMA.TABLE",
            "ROLE",
            "USER",
            false,
            false,
            false);

    // When/Then: Should throw for invalid object type
    assertThrows(Exception.class, () -> SnowflakeGrantCompanion.from(model));
  }

  @Test
  void from_shouldHandleEmptyObjectName() {
    // Given
    SnowflakeGrantModel model =
        new SnowflakeGrantModel("SELECT", "ACCOUNT", "", "ROLE", "USER", false, false, false);

    // When
    SnowflakeGrant result = SnowflakeGrantCompanion.from(model);

    // Then
    assertEquals("", result.name().value());
  }

  @Test
  void from_shouldPreservePrivilegeCase() {
    SnowflakeGrantModel model =
        new SnowflakeGrantModel(
            "select", "TABLE", "DB.SCHEMA.TABLE", "ROLE", "USER", false, false, false);

    SnowflakeGrant result = SnowflakeGrantCompanion.from(model);

    assertEquals("SELECT", result.privilege().value());
  }

  @Test
  void from_shouldMapAgentToCortexAgent() {
    SnowflakeGrantModel model =
        new SnowflakeGrantModel(
            "USAGE", "AGENT", "DB.SCHEMA.AGENT_NAME", "ROLE", "USER", false, false, false);

    SnowflakeGrant result = SnowflakeGrantCompanion.from(model);

    assertEquals(SnowflakeObjectType.CORTEX_AGENT, result.snowflakeObjectType());
  }

  @Test
  void from_shouldMapAgentCaseInsensitively() {
    SnowflakeGrantModel model =
        new SnowflakeGrantModel(
            "USAGE", "agent", "DB.SCHEMA.AGENT_NAME", "ROLE", "USER", false, false, false);

    SnowflakeGrant result = SnowflakeGrantCompanion.from(model);

    assertEquals(SnowflakeObjectType.CORTEX_AGENT, result.snowflakeObjectType());
  }

  @Test
  void from_shouldHandleCortexAgentDirectly() {
    SnowflakeGrantModel model =
        new SnowflakeGrantModel(
            "USAGE", "CORTEX_AGENT", "DB.SCHEMA.AGENT_NAME", "ROLE", "USER", false, false, false);

    SnowflakeGrant result = SnowflakeGrantCompanion.from(model);

    assertEquals(SnowflakeObjectType.CORTEX_AGENT, result.snowflakeObjectType());
  }
}
