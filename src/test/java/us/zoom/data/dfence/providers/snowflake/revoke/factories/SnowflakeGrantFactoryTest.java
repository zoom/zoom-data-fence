package us.zoom.data.dfence.providers.snowflake.revoke.factories;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import us.zoom.data.dfence.providers.snowflake.grant.builder.SnowflakeObjectType;
import us.zoom.data.dfence.providers.snowflake.models.SnowflakeGrantModel;
import us.zoom.data.dfence.providers.snowflake.revoke.models.SnowflakeGrant;
import us.zoom.data.dfence.providers.snowflake.revoke.models.SnowflakeGrantType;

class SnowflakeGrantFactoryTest {

  @Test
  void from_shouldConvertStandardGrant_whenFutureAndAllAreFalse() {
    // Given
    SnowflakeGrantModel model =
        new SnowflakeGrantModel(
            "SELECT", "TABLE", "DB.SCHEMA.TABLE", "ROLE", "USER", false, false, false);

    // When
    SnowflakeGrant result = SnowflakeGrantFactory.createFrom(model);

    // Then
    assertInstanceOf(SnowflakeGrantType.Standard.class, result.type());
    assertEquals("SELECT", result.privilege().value());
  }

  @Test
  void from_shouldConvertFutureGrant_whenFutureIsTrueAndAllIsFalse() {
    // Given
    SnowflakeGrantModel model =
        new SnowflakeGrantModel("SELECT", "TABLE", "DB.SCHEMA", "ROLE", "USER", false, true, false);

    // When
    SnowflakeGrant result = SnowflakeGrantFactory.createFrom(model);

    // Then
    assertInstanceOf(SnowflakeGrantType.Container.class, result.type());
  }

  @Test
  void from_shouldThrow_whenAllIsTrue() {
    // Given
    SnowflakeGrantModel model =
        new SnowflakeGrantModel("SELECT", "TABLE", "DB.SCHEMA", "ROLE", "USER", false, false, true);

    // When/Then
    assertThrows(Exception.class, () -> SnowflakeGrantFactory.createFrom(model));
  }

  @Test
  void from_shouldThrow_whenBothFutureAndAllAreTrue() {
    // Given: When both are true, it throws an error (invalid state)
    SnowflakeGrantModel model =
        new SnowflakeGrantModel("SELECT", "TABLE", "DB.SCHEMA", "ROLE", "USER", false, true, true);

    // When/Then: Should throw when both future and all are true
    assertThrows(Exception.class, () -> SnowflakeGrantFactory.createFrom(model));
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
    assertThrows(Exception.class, () -> SnowflakeGrantFactory.createFrom(model));
  }

  @Test
  void from_shouldHandleEmptyObjectName() {
    // Given
    SnowflakeGrantModel model =
        new SnowflakeGrantModel("SELECT", "ACCOUNT", "", "ROLE", "USER", false, false, false);

    // When
    SnowflakeGrant result = SnowflakeGrantFactory.createFrom(model);

    // Then
    // For ACCOUNT object, name is empty, type is Standard.Global which has empty parts
    assertInstanceOf(SnowflakeGrantType.Standard.Global.class, result.type());
  }

  @Test
  void from_shouldPreservePrivilegeCase() {
    SnowflakeGrantModel model =
        new SnowflakeGrantModel(
            "select", "TABLE", "DB.SCHEMA.TABLE", "ROLE", "USER", false, false, false);

    SnowflakeGrant result = SnowflakeGrantFactory.createFrom(model);

    assertEquals("SELECT", result.privilege().value());
  }

  @Test
  void from_shouldMapAgentToCortexAgent() {
    SnowflakeGrantModel model =
        new SnowflakeGrantModel(
            "USAGE", "AGENT", "DB.SCHEMA.AGENT_NAME", "ROLE", "USER", false, false, false);

    SnowflakeGrant result = SnowflakeGrantFactory.createFrom(model);

    assertEquals(SnowflakeObjectType.CORTEX_AGENT, result.snowflakeObjectType());
  }

  @Test
  void from_shouldMapAgentCaseInsensitively() {
    SnowflakeGrantModel model =
        new SnowflakeGrantModel(
            "USAGE", "agent", "DB.SCHEMA.AGENT_NAME", "ROLE", "USER", false, false, false);

    SnowflakeGrant result = SnowflakeGrantFactory.createFrom(model);

    assertEquals(SnowflakeObjectType.CORTEX_AGENT, result.snowflakeObjectType());
  }

  @Test
  void from_shouldHandleCortexAgentDirectly() {
    SnowflakeGrantModel model =
        new SnowflakeGrantModel(
            "USAGE", "CORTEX_AGENT", "DB.SCHEMA.AGENT_NAME", "ROLE", "USER", false, false, false);

    SnowflakeGrant result = SnowflakeGrantFactory.createFrom(model);

    assertEquals(SnowflakeObjectType.CORTEX_AGENT, result.snowflakeObjectType());
  }

  @Test
  void from_shouldValidateContainerQualLevel1() {
    // Valid: Object Schema (level 2) -> Container DB (level 1)
    SnowflakeGrantModel model =
        new SnowflakeGrantModel("USAGE", "SCHEMA", "DB", "ROLE", "USER", false, true, false);
    SnowflakeGrant result = SnowflakeGrantFactory.createFrom(model);
    assertInstanceOf(SnowflakeGrantType.Container.AccountObject.class, result.type());
  }

  @Test
  void from_shouldValidateContainerQualLevel2() {
    // Valid: Object Table (level 3) -> Container Schema (level 2)
    SnowflakeGrantModel model =
        new SnowflakeGrantModel("SELECT", "TABLE", "DB.SCHEMA", "ROLE", "USER", false, true, false);
    SnowflakeGrant result = SnowflakeGrantFactory.createFrom(model);
    assertInstanceOf(SnowflakeGrantType.Container.Schema.class, result.type());
  }
}
