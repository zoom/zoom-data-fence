package us.zoom.data.dfence.policies.factories;

import static org.junit.jupiter.api.Assertions.*;

import com.google.common.collect.ImmutableList;
import org.junit.jupiter.api.Test;
import us.zoom.data.dfence.exception.RbacDataError;
import us.zoom.data.dfence.playbook.model.PlaybookPrivilegeGrant;
import us.zoom.data.dfence.policies.models.PolicyGrant;
import us.zoom.data.dfence.policies.pattern.models.ContainerPolicyOption;
import us.zoom.data.dfence.policies.pattern.models.ContainerPolicyOptions;
import us.zoom.data.dfence.policies.pattern.models.PolicyType;
import us.zoom.data.dfence.providers.snowflake.grant.builder.SnowflakeObjectType;

class PolicyGrantFactoryTest {

  @Test
  void from_shouldReturnStandard_whenBothFlagsAreFalse() {
    PlaybookPrivilegeGrant grant =
        new PlaybookPrivilegeGrant(
            "table",
            "MY_TABLE",
            "MY_SCHEMA",
            "MY_DB",
            ImmutableList.of("SELECT"),
            false,
            false,
            true);

    PolicyGrant result = PolicyGrantFactory.createFrom(grant).getOrElseThrow(AssertionError::new);

    assertTrue(result.policyType() instanceof PolicyType.Standard);
    assertEquals(SnowflakeObjectType.TABLE, result.objectType());
  }

  @Test
  void from_shouldReturnFuture_whenIncludeFutureIsTrue() {
    PlaybookPrivilegeGrant grant =
        new PlaybookPrivilegeGrant(
            "table", "*", "MY_SCHEMA", "MY_DB", ImmutableList.of("SELECT"), true, false, true);

    PolicyGrant result = PolicyGrantFactory.createFrom(grant).getOrElseThrow(AssertionError::new);

    assertTrue(result.policyType() instanceof PolicyType.Container);
    PolicyType.Container container = (PolicyType.Container) result.policyType();
    assertTrue(container instanceof PolicyType.Container.Schema);
    PolicyType.Container.Schema schema = (PolicyType.Container.Schema) container;
    assertEquals(
        ContainerPolicyOptions.of(ContainerPolicyOption.FUTURE), schema.containerPolicyOptions());
  }

  @Test
  void from_shouldReturnAll_whenIncludeAllIsTrue() {
    PlaybookPrivilegeGrant grant =
        new PlaybookPrivilegeGrant(
            "table", "*", "MY_SCHEMA", "MY_DB", ImmutableList.of("SELECT"), false, true, true);

    PolicyGrant result = PolicyGrantFactory.createFrom(grant).getOrElseThrow(AssertionError::new);

    assertTrue(result.policyType() instanceof PolicyType.Container);
    PolicyType.Container container = (PolicyType.Container) result.policyType();
    assertTrue(container instanceof PolicyType.Container.Schema);
    PolicyType.Container.Schema schema = (PolicyType.Container.Schema) container;
    assertEquals(
        ContainerPolicyOptions.of(ContainerPolicyOption.ALL), schema.containerPolicyOptions());
  }

  @Test
  void from_returnsSchemaObjectAllSchemas_whenFutureAndAllTrue() {
    PlaybookPrivilegeGrant grant =
        new PlaybookPrivilegeGrant(
            "table", "*", "*", "MY_DB", ImmutableList.of("SELECT"), true, true, true);

    PolicyGrant result = PolicyGrantFactory.createFrom(grant).getOrElseThrow(AssertionError::new);

    assertTrue(result.policyType() instanceof PolicyType.Container);
    PolicyType.Container container = (PolicyType.Container) result.policyType();
    assertTrue(container instanceof PolicyType.Container.SchemaObjectAllSchemas);
    PolicyType.Container.SchemaObjectAllSchemas dbLevel =
        (PolicyType.Container.SchemaObjectAllSchemas) container;
    assertEquals("MY_DB", dbLevel.databaseName());
    assertEquals(
        ContainerPolicyOptions.of(ContainerPolicyOption.ALL, ContainerPolicyOption.FUTURE),
        dbLevel.containerPolicyOptions());
  }

  @Test
  void from_returnsStandardForDatabase_whenFutureAndIncludeAllFalse() {
    PlaybookPrivilegeGrant grant =
        new PlaybookPrivilegeGrant(
            "database", null, null, "MY_DB", ImmutableList.of("USAGE"), false, false, true);

    PolicyGrant result = PolicyGrantFactory.createFrom(grant).getOrElseThrow(AssertionError::new);

    assertTrue(result.policyType() instanceof PolicyType.Standard);
    assertEquals(SnowflakeObjectType.DATABASE, result.objectType());
  }

  @Test
  void from_shouldThrow_whenObjectTypeIsInvalid() {
    PlaybookPrivilegeGrant grant =
        new PlaybookPrivilegeGrant(
            "INVALID_TYPE", "OBJ", "SCH", "DB", ImmutableList.of("SELECT"), false, false, true);

    assertThrows(RbacDataError.class, () -> PolicyGrantFactory.createFrom(grant));
  }

  @Test
  void from_shouldTrimAndFilterEmptyStrings() {
    PlaybookPrivilegeGrant grant =
        new PlaybookPrivilegeGrant(
            "table",
            "  MY_TABLE  ",
            "  MY_SCHEMA  ",
            "  MY_DB  ",
            ImmutableList.of("SELECT"),
            false,
            false,
            true);

    PolicyGrant result = PolicyGrantFactory.createFrom(grant).getOrElseThrow(AssertionError::new);

    // Assert using policyType properties
    assertTrue(result.policyType() instanceof PolicyType.Standard.SchemaObject);
    PolicyType.Standard.SchemaObject pattern =
        (PolicyType.Standard.SchemaObject) result.policyType();
    assertEquals("MY_TABLE", pattern.objectName());
    assertEquals("MY_SCHEMA", pattern.schemaName());
    assertEquals("MY_DB", pattern.databaseName());
  }

  @Test
  void from_returnsAccountObject_whenDatabaseOnlyAndNullSchemaAndObject() {
    PlaybookPrivilegeGrant grant =
        new PlaybookPrivilegeGrant(
            "database", null, null, "MY_DB", ImmutableList.of("USAGE"), false, false, true);

    PolicyGrant result = PolicyGrantFactory.createFrom(grant).getOrElseThrow(AssertionError::new);

    assertTrue(result.policyType() instanceof PolicyType.Standard.AccountObject);
    PolicyType.Standard.AccountObject pattern =
        (PolicyType.Standard.AccountObject) result.policyType();
    assertEquals("MY_DB", pattern.objectName());
  }
}
