package us.zoom.data.dfence.providers.snowflake.revoke.companions;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Optional;
import org.junit.jupiter.api.Test;
import us.zoom.data.dfence.providers.snowflake.grant.builder.SnowflakeObjectType;
import us.zoom.data.dfence.providers.snowflake.models.SnowflakeGrantModel;
import us.zoom.data.dfence.providers.snowflake.revoke.models.SnowflakeGrant;
import us.zoom.data.dfence.providers.snowflake.revoke.models.SnowflakeGrantType;

class SnowflakeGrantCompanionTest {

  @Test
  void from_shouldConvertStandardGrant_whenFutureAndAllAreFalse() {
    SnowflakeGrantModel model =
        new SnowflakeGrantModel(
            "SELECT", "TABLE", "DB.SCHEMA.TABLE", "ROLE", "USER", false, false, false);

    Optional<SnowflakeGrant> result = SnowflakeGrantCompanion.from(model);

    assertTrue(result.isPresent());
    assertEquals(SnowflakeGrantType.Standard, result.get().grantType());
    assertEquals("SELECT", result.get().privilege().value());
  }

  @Test
  void from_shouldConvertFutureGrant_whenFutureIsTrueAndAllIsFalse() {
    SnowflakeGrantModel model =
        new SnowflakeGrantModel("SELECT", "TABLE", "DB.SCHEMA", "ROLE", "USER", false, true, false);

    Optional<SnowflakeGrant> result = SnowflakeGrantCompanion.from(model);

    assertTrue(result.isPresent());
    assertEquals(SnowflakeGrantType.Future, result.get().grantType());
  }

  @Test
  void from_shouldConvertAllGrant_whenAllIsTrueAndFutureIsFalse() {
    SnowflakeGrantModel model =
        new SnowflakeGrantModel("SELECT", "TABLE", "DB.SCHEMA", "ROLE", "USER", false, false, true);

    Optional<SnowflakeGrant> result = SnowflakeGrantCompanion.from(model);

    assertTrue(result.isPresent());
    assertEquals(SnowflakeGrantType.All, result.get().grantType());
  }

  @Test
  void from_shouldConvertStandardGrant_whenBothFutureAndAllAreTrue() {
    SnowflakeGrantModel model =
        new SnowflakeGrantModel("SELECT", "TABLE", "DB.SCHEMA", "ROLE", "USER", false, true, true);

    Optional<SnowflakeGrant> result = SnowflakeGrantCompanion.from(model);

    assertTrue(result.isPresent());
    assertEquals(SnowflakeGrantType.Standard, result.get().grantType());
  }

  @Test
  void from_shouldReturnEmpty_whenObjectTypeIsInvalid() {
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

    Optional<SnowflakeGrant> result = SnowflakeGrantCompanion.from(model);

    assertTrue(result.isEmpty(), "Should return empty for invalid object type");
  }

  @Test
  void from_shouldHandleEmptyObjectName() {
    SnowflakeGrantModel model =
        new SnowflakeGrantModel("SELECT", "ACCOUNT", "", "ROLE", "USER", false, false, false);

    Optional<SnowflakeGrant> result = SnowflakeGrantCompanion.from(model);

    assertTrue(result.isPresent(), "Should handle empty object name for account-level grants");
    assertEquals("", result.get().name().value());
  }

  @Test
  void from_shouldPreservePrivilegeCase() {
    SnowflakeGrantModel model =
        new SnowflakeGrantModel(
            "select", "TABLE", "DB.SCHEMA.TABLE", "ROLE", "USER", false, false, false);

    Optional<SnowflakeGrant> result = SnowflakeGrantCompanion.from(model);

    assertTrue(result.isPresent());
    assertEquals(
        "SELECT", result.get().privilege().value(), "Privilege should be normalized to uppercase");
  }

  @Test
  void from_shouldMapAgentToCortexAgent() {
    // Critical: AGENT string should be mapped to CORTEX_AGENT enum via fromString() override
    SnowflakeGrantModel model =
        new SnowflakeGrantModel(
            "USAGE", "AGENT", "DB.SCHEMA.AGENT_NAME", "ROLE", "USER", false, false, false);

    Optional<SnowflakeGrant> result = SnowflakeGrantCompanion.from(model);

    assertTrue(result.isPresent(), "Should successfully convert AGENT to CORTEX_AGENT");
    assertEquals(
        SnowflakeObjectType.CORTEX_AGENT,
        result.get().snowflakeObjectType(),
        "AGENT string should be mapped to CORTEX_AGENT enum");
  }

  @Test
  void from_shouldMapAgentCaseInsensitively() {
    // Critical: Case should not matter for AGENT override
    SnowflakeGrantModel model =
        new SnowflakeGrantModel(
            "USAGE", "agent", "DB.SCHEMA.AGENT_NAME", "ROLE", "USER", false, false, false);

    Optional<SnowflakeGrant> result = SnowflakeGrantCompanion.from(model);

    assertTrue(result.isPresent());
    assertEquals(
        SnowflakeObjectType.CORTEX_AGENT,
        result.get().snowflakeObjectType(),
        "Lowercase 'agent' should be mapped to CORTEX_AGENT enum");
  }

  @Test
  void from_shouldHandleCortexAgentDirectly() {
    // Critical: CORTEX_AGENT string should work directly
    SnowflakeGrantModel model =
        new SnowflakeGrantModel(
            "USAGE", "CORTEX_AGENT", "DB.SCHEMA.AGENT_NAME", "ROLE", "USER", false, false, false);

    Optional<SnowflakeGrant> result = SnowflakeGrantCompanion.from(model);

    assertTrue(result.isPresent());
    assertEquals(
        SnowflakeObjectType.CORTEX_AGENT,
        result.get().snowflakeObjectType(),
        "CORTEX_AGENT string should map directly to CORTEX_AGENT enum");
  }
}
