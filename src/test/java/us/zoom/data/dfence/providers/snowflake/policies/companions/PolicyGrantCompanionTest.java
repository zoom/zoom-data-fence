package us.zoom.data.dfence.providers.snowflake.policies.companions;

import static org.junit.jupiter.api.Assertions.*;

import com.google.common.collect.ImmutableList;
import org.junit.jupiter.api.Test;
import us.zoom.data.dfence.exception.RbacDataError;
import us.zoom.data.dfence.playbook.model.PlaybookPrivilegeGrant;
import us.zoom.data.dfence.policies.companions.PolicyGrantCompanion;
import us.zoom.data.dfence.providers.snowflake.grant.builder.SnowflakeObjectType;
import us.zoom.data.dfence.policies.pattern.models.ContainerPatternOption;
import us.zoom.data.dfence.policies.pattern.models.ContainerPatternOptions;
import us.zoom.data.dfence.policies.pattern.models.ResolvedPolicyPattern;
import us.zoom.data.dfence.policies.models.PolicyGrant;

class PolicyGrantCompanionTest {

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

    PolicyGrant result = PolicyGrantCompanion.from(grant);

    assertTrue(result.resolvedPattern() instanceof ResolvedPolicyPattern.Standard);
    assertEquals(SnowflakeObjectType.TABLE, result.objectType());
  }

  @Test
  void from_shouldReturnFuture_whenIncludeFutureIsTrue() {
    PlaybookPrivilegeGrant grant =
        new PlaybookPrivilegeGrant(
            "table", "*", "MY_SCHEMA", "MY_DB", ImmutableList.of("SELECT"), true, false, true);

    PolicyGrant result = PolicyGrantCompanion.from(grant);

    assertTrue(result.resolvedPattern() instanceof ResolvedPolicyPattern.Container);
    ResolvedPolicyPattern.Container container =
        (ResolvedPolicyPattern.Container) result.resolvedPattern();
    assertTrue(container instanceof ResolvedPolicyPattern.Container.Schema);
    ResolvedPolicyPattern.Container.Schema schema =
        (ResolvedPolicyPattern.Container.Schema) container;
    assertEquals(
        ContainerPatternOptions.of(ContainerPatternOption.FUTURE),
        schema.containerPatternOptions());
  }

  @Test
  void from_shouldReturnAll_whenIncludeAllIsTrue() {
    PlaybookPrivilegeGrant grant =
        new PlaybookPrivilegeGrant(
            "table", "*", "MY_SCHEMA", "MY_DB", ImmutableList.of("SELECT"), false, true, true);

    PolicyGrant result = PolicyGrantCompanion.from(grant);

    assertTrue(result.resolvedPattern() instanceof ResolvedPolicyPattern.Container);
    ResolvedPolicyPattern.Container container =
        (ResolvedPolicyPattern.Container) result.resolvedPattern();
    assertTrue(container instanceof ResolvedPolicyPattern.Container.Schema);
    ResolvedPolicyPattern.Container.Schema schema =
        (ResolvedPolicyPattern.Container.Schema) container;
    assertEquals(
        ContainerPatternOptions.of(ContainerPatternOption.ALL), schema.containerPatternOptions());
  }

  @Test
  void from_returnsFutureAndAll_whenIncludeFutureAndIncludeAllTrue() {
    PlaybookPrivilegeGrant grant =
        new PlaybookPrivilegeGrant(
            "table", "*", "*", "MY_DB", ImmutableList.of("SELECT"), true, true, true);

    PolicyGrant result = PolicyGrantCompanion.from(grant);

    assertTrue(result.resolvedPattern() instanceof ResolvedPolicyPattern.Container);
    ResolvedPolicyPattern.Container container =
        (ResolvedPolicyPattern.Container) result.resolvedPattern();
    assertTrue(container instanceof ResolvedPolicyPattern.Container.AccountObjectDatabase);
    ResolvedPolicyPattern.Container.AccountObjectDatabase dbLevel =
        (ResolvedPolicyPattern.Container.AccountObjectDatabase) container;
    assertEquals(
        ContainerPatternOptions.of(ContainerPatternOption.ALL, ContainerPatternOption.FUTURE),
        dbLevel.containerPatternOptions());
  }

  @Test
  void from_returnsStandardForDatabase_whenFutureAndIncludeAllFalse() {
    PlaybookPrivilegeGrant grant =
        new PlaybookPrivilegeGrant(
            "database", null, null, "MY_DB", ImmutableList.of("USAGE"), false, false, true);

    PolicyGrant result = PolicyGrantCompanion.from(grant);

    assertTrue(result.resolvedPattern() instanceof ResolvedPolicyPattern.Standard);
    assertEquals(SnowflakeObjectType.DATABASE, result.objectType());
  }

  @Test
  void from_shouldThrow_whenObjectTypeIsInvalid() {
    PlaybookPrivilegeGrant grant =
        new PlaybookPrivilegeGrant(
            "INVALID_TYPE", "OBJ", "SCH", "DB", ImmutableList.of("SELECT"), false, false, true);

    assertThrows(RbacDataError.class, () -> PolicyGrantCompanion.from(grant));
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

    PolicyGrant result = PolicyGrantCompanion.from(grant);

    assertEquals(io.vavr.control.Option.some("MY_TABLE"), result.pattern().objName());
    assertEquals(io.vavr.control.Option.some("MY_SCHEMA"), result.pattern().schName());
    assertEquals(io.vavr.control.Option.some("MY_DB"), result.pattern().dbName());
  }

  @Test
  void from_shouldHandleNullValues() {
    PlaybookPrivilegeGrant grant =
        new PlaybookPrivilegeGrant(
            "database", null, null, "MY_DB", ImmutableList.of("USAGE"), false, false, true);

    PolicyGrant result = PolicyGrantCompanion.from(grant);

    assertTrue(result.pattern().objName().isEmpty());
    assertTrue(result.pattern().schName().isEmpty());
    assertTrue(result.pattern().dbName().isDefined());
  }
}
