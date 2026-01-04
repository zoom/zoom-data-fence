package us.zoom.data.dfence.providers.snowflake.revoke.matchers;

import static org.junit.jupiter.api.Assertions.*;

import com.google.common.collect.ImmutableList;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import us.zoom.data.dfence.providers.snowflake.grant.builder.SnowflakeObjectType;
import us.zoom.data.dfence.providers.snowflake.models.SnowflakeGrantModel;
import us.zoom.data.dfence.providers.snowflake.revoke.companions.SnowflakeGrantCompanion;
import us.zoom.data.dfence.providers.snowflake.revoke.matchers.playbook.PlaybookGrantMatcher;
import us.zoom.data.dfence.providers.snowflake.revoke.models.PlaybookGrant;
import us.zoom.data.dfence.providers.snowflake.revoke.models.PlaybookGrantType;
import us.zoom.data.dfence.providers.snowflake.revoke.models.PlaybookPattern;
import us.zoom.data.dfence.providers.snowflake.revoke.models.SnowflakeGrant;
import us.zoom.data.dfence.providers.snowflake.revoke.models.wrappers.GrantPrivilege;

class PlaybookGrantMatcherTest {

  @Test
  void matchGrant_AgainstPlaybook_shouldReturnTrue_whenAllFieldsMatchExactly() {
    SnowflakeGrant snowGrant = createSnowGrant("SELECT", "TABLE", "DB.SCHEMA.TABLE");
    PlaybookGrant playbookGrant =
        createPlaybookGrant("SELECT", SnowflakeObjectType.TABLE, "DB", "SCHEMA", "TABLE");
    boolean actualMatch =
        PlaybookGrantMatcher.matchGrantAgainstPlaybook().apply(playbookGrant, snowGrant);

    assertTrue(actualMatch, "Should match when privilege, object type, and object name all match");
  }

  @Test
  void matchGrant_AgainstPlaybook_shouldReturnFalse_whenPrivilegeDiffers() {
    SnowflakeGrant snowGrant = createSnowGrant("UPDATE", "TABLE", "DB.SCHEMA.TABLE");
    PlaybookGrant playbookGrant =
        createPlaybookGrant("SELECT", SnowflakeObjectType.TABLE, "DB", "SCHEMA", "TABLE");
    boolean actualMatch =
        PlaybookGrantMatcher.matchGrantAgainstPlaybook().apply(playbookGrant, snowGrant);

    assertFalse(actualMatch, "Should not match when privilege differs");
  }

  @Test
  void matchGrant_AgainstPlaybook_shouldReturnFalse_whenObjectTypeDiffers() {
    SnowflakeGrant snowGrant = createSnowGrant("SELECT", "VIEW", "DB.SCHEMA.TABLE");
    PlaybookGrant playbookGrant =
        createPlaybookGrant("SELECT", SnowflakeObjectType.TABLE, "DB", "SCHEMA", "TABLE");
    boolean actualMatch =
        PlaybookGrantMatcher.matchGrantAgainstPlaybook().apply(playbookGrant, snowGrant);

    assertFalse(actualMatch, "Should not match when object type differs");
  }

  @Test
  void matchGrant_AgainstPlaybook_shouldReturnFalse_whenObjectNameDiffers() {
    SnowflakeGrant snowGrant = createSnowGrant("SELECT", "TABLE", "DB.SCHEMA.OTHER_TABLE");
    PlaybookGrant playbookGrant =
        createPlaybookGrant("SELECT", SnowflakeObjectType.TABLE, "DB", "SCHEMA", "TABLE");
    boolean actualMatch =
        PlaybookGrantMatcher.matchGrantAgainstPlaybook().apply(playbookGrant, snowGrant);

    assertFalse(actualMatch, "Should not match when object name differs");
  }

  @Test
  void matchAgainstPlaybook() {
    SnowflakeGrant snowGrant = createSnowGrant("SELECT", "TABLE", "DB.SCHEMA.ANY_TABLE");
    PlaybookGrant playbookGrant =
        createPlaybookGrant("SELECT", SnowflakeObjectType.TABLE, "DB", "SCHEMA", null);
    boolean actualMatch =
        PlaybookGrantMatcher.matchGrantAgainstPlaybook().apply(playbookGrant, snowGrant);

    assertTrue(actualMatch, "Should match when playbook has wildcard (null) object name");
  }

  @Test
  void matchGrant_AgainstPlaybook_shouldReturnTrue_whenObjectTypesShareAlias() {
    // Critical: EXTERNAL_TABLE and TABLE share the same alias, so they should match
    SnowflakeGrant snowGrant = createSnowGrant("SELECT", "EXTERNAL_TABLE", "DB.SCHEMA.EXT_TABLE");
    PlaybookGrant playbookGrant =
        createPlaybookGrant("SELECT", SnowflakeObjectType.TABLE, "DB", "SCHEMA", "EXT_TABLE");
    boolean actualMatch =
        PlaybookGrantMatcher.matchGrantAgainstPlaybook().apply(playbookGrant, snowGrant);

    assertTrue(
        actualMatch,
        "Should match when object types share the same alias (EXTERNAL_TABLE and TABLE)");
  }

  @Test
  void matchTypeAgainstPlaybook() {
    // Critical: Future grants are compatible with Future playbook grants
    SnowflakeGrant snowGrant = createSnowGrant("SELECT", "TABLE", "DB.SCHEMA", true, false);
    PlaybookGrant playbookGrant =
        createPlaybookGrant(
            "SELECT", SnowflakeObjectType.TABLE, "DB", "SCHEMA", null, PlaybookGrantType.FUTURE);
    boolean actualMatch =
        PlaybookGrantMatcher.matchGrantAgainstPlaybook().apply(playbookGrant, snowGrant);

    assertTrue(actualMatch, "Should match when Future grant is compatible with Future playbook");
  }

  // Helper Functions
  private SnowflakeGrant createSnowGrant(
      String privilege, String objectType, String objectName, boolean future, boolean all) {
    SnowflakeGrantModel model =
        new SnowflakeGrantModel(
            privilege, objectType, objectName, "ROLE", "GRANTEE", false, future, all);
    Optional<SnowflakeGrant> grant = SnowflakeGrantCompanion.from(model);
    return grant.orElseThrow(
        () ->
            new RuntimeException("Unable to build snow grant from snowflake grant model " + model));
  }

  private SnowflakeGrant createSnowGrant(String privilege, String objectType, String objectName) {
    return createSnowGrant(privilege, objectType, objectName, false, false);
  }

  private PlaybookGrant createPlaybookGrant(
      String privilege,
      SnowflakeObjectType objectType,
      String db,
      String schema,
      String obj,
      PlaybookGrantType grantType) {
    return new PlaybookGrant(
        objectType,
        new PlaybookPattern(
            Optional.ofNullable(db), Optional.ofNullable(schema), Optional.ofNullable(obj)),
        ImmutableList.of(new GrantPrivilege(privilege)),
        grantType,
        true);
  }

  private PlaybookGrant createPlaybookGrant(
      String privilege, SnowflakeObjectType objectType, String db, String schema, String obj) {
    return createPlaybookGrant(privilege, objectType, db, schema, obj, PlaybookGrantType.STANDARD);
  }
}
