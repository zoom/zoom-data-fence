package us.zoom.data.dfence.providers.snowflake.shared.companions;

import static org.junit.jupiter.api.Assertions.*;

import com.google.common.collect.ImmutableList;
import org.junit.jupiter.api.Test;
import us.zoom.data.dfence.exception.RbacDataError;
import us.zoom.data.dfence.playbook.model.PlaybookPrivilegeGrant;
import us.zoom.data.dfence.providers.snowflake.grant.builder.SnowflakeObjectType;
import us.zoom.data.dfence.providers.snowflake.grant.desired.create.validations.playbook.pattern.models.ContainerPatternOption;
import us.zoom.data.dfence.providers.snowflake.grant.desired.create.validations.playbook.pattern.models.ContainerPatternOptions;
import us.zoom.data.dfence.providers.snowflake.grant.desired.create.validations.playbook.pattern.models.ResolvedPlaybookPattern;
import us.zoom.data.dfence.providers.snowflake.shared.models.PlaybookGrant;

class PlaybookGrantCompanionTest {

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

    PlaybookGrant result = PlaybookGrantCompanion.from(grant);

    assertTrue(result.resolvedPattern() instanceof ResolvedPlaybookPattern.Standard);
    assertEquals(SnowflakeObjectType.TABLE, result.objectType());
  }

  @Test
  void from_shouldReturnFuture_whenIncludeFutureIsTrue() {
    PlaybookPrivilegeGrant grant =
        new PlaybookPrivilegeGrant(
            "table", "*", "MY_SCHEMA", "MY_DB", ImmutableList.of("SELECT"), true, false, true);

    PlaybookGrant result = PlaybookGrantCompanion.from(grant);

    assertTrue(result.resolvedPattern() instanceof ResolvedPlaybookPattern.Container);
    ResolvedPlaybookPattern.Container container =
        (ResolvedPlaybookPattern.Container) result.resolvedPattern();
    assertTrue(container instanceof ResolvedPlaybookPattern.Container.Schema);
    ResolvedPlaybookPattern.Container.Schema schema =
        (ResolvedPlaybookPattern.Container.Schema) container;
    assertEquals(
        ContainerPatternOptions.of(ContainerPatternOption.FUTURE),
        schema.containerPatternOptions());
  }

  @Test
  void from_shouldReturnAll_whenIncludeAllIsTrue() {
    PlaybookPrivilegeGrant grant =
        new PlaybookPrivilegeGrant(
            "table", "*", "MY_SCHEMA", "MY_DB", ImmutableList.of("SELECT"), false, true, true);

    PlaybookGrant result = PlaybookGrantCompanion.from(grant);

    assertTrue(result.resolvedPattern() instanceof ResolvedPlaybookPattern.Container);
    ResolvedPlaybookPattern.Container container =
        (ResolvedPlaybookPattern.Container) result.resolvedPattern();
    assertTrue(container instanceof ResolvedPlaybookPattern.Container.Schema);
    ResolvedPlaybookPattern.Container.Schema schema =
        (ResolvedPlaybookPattern.Container.Schema) container;
    assertEquals(
        ContainerPatternOptions.of(ContainerPatternOption.ALL), schema.containerPatternOptions());
  }

  @Test
  void from_returnsFutureAndAll_whenIncludeFutureAndIncludeAllTrue() {
    PlaybookPrivilegeGrant grant =
        new PlaybookPrivilegeGrant(
            "table", "*", "*", "MY_DB", ImmutableList.of("SELECT"), true, true, true);

    PlaybookGrant result = PlaybookGrantCompanion.from(grant);

    assertTrue(result.resolvedPattern() instanceof ResolvedPlaybookPattern.Container);
    ResolvedPlaybookPattern.Container container =
        (ResolvedPlaybookPattern.Container) result.resolvedPattern();
    assertTrue(container instanceof ResolvedPlaybookPattern.Container.AccountObjectDatabase);
    ResolvedPlaybookPattern.Container.AccountObjectDatabase dbLevel =
        (ResolvedPlaybookPattern.Container.AccountObjectDatabase) container;
    assertEquals(
        ContainerPatternOptions.of(ContainerPatternOption.ALL, ContainerPatternOption.FUTURE),
        dbLevel.containerPatternOptions());
  }

  @Test
  void from_returnsStandardForDatabase_whenFutureAndIncludeAllFalse() {
    PlaybookPrivilegeGrant grant =
        new PlaybookPrivilegeGrant(
            "database", null, null, "MY_DB", ImmutableList.of("USAGE"), false, false, true);

    PlaybookGrant result = PlaybookGrantCompanion.from(grant);

    assertTrue(result.resolvedPattern() instanceof ResolvedPlaybookPattern.Standard);
    assertEquals(SnowflakeObjectType.DATABASE, result.objectType());
  }

  @Test
  void from_shouldThrow_whenObjectTypeIsInvalid() {
    PlaybookPrivilegeGrant grant =
        new PlaybookPrivilegeGrant(
            "INVALID_TYPE", "OBJ", "SCH", "DB", ImmutableList.of("SELECT"), false, false, true);

    assertThrows(RbacDataError.class, () -> PlaybookGrantCompanion.from(grant));
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

    PlaybookGrant result = PlaybookGrantCompanion.from(grant);

    assertEquals(io.vavr.control.Option.some("MY_TABLE"), result.pattern().objName());
    assertEquals(io.vavr.control.Option.some("MY_SCHEMA"), result.pattern().schName());
    assertEquals(io.vavr.control.Option.some("MY_DB"), result.pattern().dbName());
  }

  @Test
  void from_shouldHandleNullValues() {
    PlaybookPrivilegeGrant grant =
        new PlaybookPrivilegeGrant(
            "database", null, null, "MY_DB", ImmutableList.of("USAGE"), false, false, true);

    PlaybookGrant result = PlaybookGrantCompanion.from(grant);

    assertTrue(result.pattern().objName().isEmpty());
    assertTrue(result.pattern().schName().isEmpty());
    assertTrue(result.pattern().dbName().isDefined());
  }
}
