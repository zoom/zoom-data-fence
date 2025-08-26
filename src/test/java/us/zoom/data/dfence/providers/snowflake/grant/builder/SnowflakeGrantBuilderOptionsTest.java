package us.zoom.data.dfence.providers.snowflake.grant.builder;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

import us.zoom.data.dfence.providers.snowflake.grant.builder.options.SnowflakeGrantBuilderOptions;
import us.zoom.data.dfence.providers.snowflake.grant.builder.options.UnsupportedRevokeBehavior;
import us.zoom.data.dfence.providers.snowflake.models.SnowflakeGrantModel;

class SnowflakeGrantBuilderOptionsTest {

  @Test
  void testFromGrantWithDefaultOptions() {
    SnowflakeGrantModel grant =
        new SnowflakeGrantModel(
            "SELECT",
            "TABLE",
            "TEST_DB.TEST_SCHEMA.TEST_TABLE",
            "ROLE",
            "TEST_ROLE",
            false,
            false,
            false);

    SnowflakeGrantBuilder builder = SnowflakeGrantBuilder.fromGrant(grant);

    assertNotNull(builder);
    assertEquals(grant, builder.getGrant());
  }

  @Test
  void testFromGrantWithSuppressErrorsTrue() {
    // Use a valid grant but with suppressErrors true to test the option
    SnowflakeGrantModel grant =
        new SnowflakeGrantModel(
            "SELECT",
            "TABLE",
            "TEST_DB.TEST_SCHEMA.TEST_TABLE",
            "ROLE",
            "TEST_ROLE",
            false,
            false,
            false);

    SnowflakeGrantBuilder builder = SnowflakeGrantBuilder.fromGrant(grant, true);

    // Should return a valid builder when suppressErrors is true
    assertNotNull(builder);
  }

  @Test
  void testFromGrantWithSuppressErrorsFalse() {
    // Use a valid grant but with suppressErrors false to test the option
    SnowflakeGrantModel grant =
        new SnowflakeGrantModel(
            "SELECT",
            "TABLE",
            "TEST_DB.TEST_SCHEMA.TEST_TABLE",
            "ROLE",
            "TEST_ROLE",
            false,
            false,
            false);

    SnowflakeGrantBuilder builder = SnowflakeGrantBuilder.fromGrant(grant, false);

    // Should return a valid builder when suppressErrors is false
    assertNotNull(builder);
  }

  @Test
  void testFromGrantWithCustomOptions() {
    SnowflakeGrantModel grant =
        new SnowflakeGrantModel(
            "SELECT",
            "TABLE",
            "TEST_DB.TEST_SCHEMA.TEST_TABLE",
            "ROLE",
            "TEST_ROLE",
            false,
            false,
            false);

    SnowflakeGrantBuilderOptions options = new SnowflakeGrantBuilderOptions();
    options.setUnsupportedRevokeBehavior(UnsupportedRevokeBehavior.DROP);
    options.setSuppressErrors(false);

    SnowflakeGrantBuilder builder = SnowflakeGrantBuilder.fromGrant(grant, options);

    assertNotNull(builder);
    assertEquals(grant, builder.getGrant());
  }

  @Test
  void testFromGrantWithCustomOptionsSuppressErrors() {
    SnowflakeGrantModel grant =
        new SnowflakeGrantModel(
            "SELECT",
            "TABLE",
            "TEST_DB.TEST_SCHEMA.TEST_TABLE",
            "ROLE",
            "TEST_ROLE",
            false,
            false,
            false);

    SnowflakeGrantBuilderOptions options = new SnowflakeGrantBuilderOptions();
    options.setUnsupportedRevokeBehavior(UnsupportedRevokeBehavior.DROP);
    options.setSuppressErrors(true);

    SnowflakeGrantBuilder builder = SnowflakeGrantBuilder.fromGrant(grant, options);

    // Should return a valid builder when suppressErrors is true
    assertNotNull(builder);
  }

  @Test
  void testFromGrantWithNullOptions() {
    SnowflakeGrantModel grant =
        new SnowflakeGrantModel(
            "SELECT",
            "TABLE",
            "TEST_DB.TEST_SCHEMA.TEST_TABLE",
            "ROLE",
            "TEST_ROLE",
            false,
            false,
            false);

    SnowflakeGrantBuilderOptions options = null;

    // Should handle null options gracefully by using defaults
    SnowflakeGrantBuilder builder = SnowflakeGrantBuilder.fromGrant(grant, options);

    assertNotNull(builder);
    assertEquals(grant, builder.getGrant());
  }

  @Test
  void testFromGrantWithValidPermissionGrant() {
    SnowflakeGrantModel grant =
        new SnowflakeGrantModel(
            "SELECT",
            "TABLE",
            "TEST_DB.TEST_SCHEMA.TEST_TABLE",
            "ROLE",
            "TEST_ROLE",
            false,
            false,
            false);

    SnowflakeGrantBuilderOptions options = new SnowflakeGrantBuilderOptions();
    options.setUnsupportedRevokeBehavior(UnsupportedRevokeBehavior.IGNORE);

    SnowflakeGrantBuilder builder = SnowflakeGrantBuilder.fromGrant(grant, options);

    assertNotNull(builder);
    assertTrue(builder.isValid());
    assertEquals(grant, builder.getGrant());
  }

  @Test
  void testFromGrantWithValidRoleGrant() {
    SnowflakeGrantModel grant =
        new SnowflakeGrantModel(
            "USAGE", "ROLE", "TEST_ROLE", "ROLE", "ANOTHER_ROLE", false, false, false);

    SnowflakeGrantBuilderOptions options = new SnowflakeGrantBuilderOptions();
    options.setUnsupportedRevokeBehavior(UnsupportedRevokeBehavior.DROP);

    SnowflakeGrantBuilder builder = SnowflakeGrantBuilder.fromGrant(grant, options);

    assertNotNull(builder);
    assertTrue(builder.isValid());
    assertEquals(grant, builder.getGrant());
  }

  @Test
  void testFromGrantWithValidDatabaseGrant() {
    SnowflakeGrantModel grant =
        new SnowflakeGrantModel(
            "USAGE", "DATABASE", "TEST_DB", "ROLE", "TEST_ROLE", false, false, false);

    SnowflakeGrantBuilderOptions options = new SnowflakeGrantBuilderOptions();
    options.setUnsupportedRevokeBehavior(UnsupportedRevokeBehavior.IGNORE);

    SnowflakeGrantBuilder builder = SnowflakeGrantBuilder.fromGrant(grant, options);

    assertNotNull(builder);
    assertTrue(builder.isValid());
    assertEquals(grant, builder.getGrant());
  }

  @Test
  void testFromGrantWithValidSchemaGrant() {
    SnowflakeGrantModel grant =
        new SnowflakeGrantModel(
            "USAGE", "SCHEMA", "TEST_DB.TEST_SCHEMA", "ROLE", "TEST_ROLE", false, false, false);

    SnowflakeGrantBuilderOptions options = new SnowflakeGrantBuilderOptions();
    options.setUnsupportedRevokeBehavior(UnsupportedRevokeBehavior.DROP);

    SnowflakeGrantBuilder builder = SnowflakeGrantBuilder.fromGrant(grant, options);

    assertNotNull(builder);
    assertTrue(builder.isValid());
    assertEquals(grant, builder.getGrant());
  }

  @Test
  void testFromGrantWithValidWarehouseGrant() {
    SnowflakeGrantModel grant =
        new SnowflakeGrantModel(
            "USAGE", "WAREHOUSE", "TEST_WAREHOUSE", "ROLE", "TEST_ROLE", false, false, false);

    SnowflakeGrantBuilderOptions options = new SnowflakeGrantBuilderOptions();
    options.setUnsupportedRevokeBehavior(UnsupportedRevokeBehavior.IGNORE);

    SnowflakeGrantBuilder builder = SnowflakeGrantBuilder.fromGrant(grant, options);

    assertNotNull(builder);
    assertTrue(builder.isValid());
    assertEquals(grant, builder.getGrant());
  }
}
