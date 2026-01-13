package us.zoom.data.dfence.providers.snowflake.revoke.matchers;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Optional;
import org.junit.jupiter.api.Test;
import us.zoom.data.dfence.providers.snowflake.revoke.models.PlaybookPattern;

class PlaybookPatternMatchersTest {

  @Test
  void accountLevel_shouldMatch_whenValueIsEmptyOrWhitespace() {
    // Critical: Account-level grants have empty object names
    PlaybookPattern pattern =
        new PlaybookPattern(Optional.empty(), Optional.empty(), Optional.empty());

    assertTrue(
        PlaybookPatternMatchers.accountLevel("").apply(pattern),
        "Empty string should match account-level grant");
    assertTrue(
        PlaybookPatternMatchers.accountLevel("   ").apply(pattern),
        "Whitespace-only string should match account-level grant");
  }

  @Test
  void accountLevel_shouldNotMatch_whenValueIsNotEmpty() {
    PlaybookPattern pattern =
        new PlaybookPattern(Optional.empty(), Optional.empty(), Optional.empty());

    assertFalse(
        PlaybookPatternMatchers.accountLevel("TEST").apply(pattern),
        "Non-empty value should not match account-level grant");
    assertFalse(
        PlaybookPatternMatchers.accountLevel("  TEST  ").apply(pattern),
        "Trimmed non-empty value should not match");
  }

  @Test
  void accountLevelObject_shouldMatch_whenObjectNameMatchesCaseInsensitively() {
    // Critical: Account-level objects have no database or schema, only object name
    PlaybookPattern pattern =
        new PlaybookPattern(Optional.empty(), Optional.empty(), Optional.of("OBJ_NAME"));

    assertTrue(
        PlaybookPatternMatchers.accountLevelObject("OBJ_NAME").apply(pattern),
        "Should match exact object name");
    assertTrue(
        PlaybookPatternMatchers.accountLevelObject("obj_name").apply(pattern),
        "Should match case-insensitively");
  }

  @Test
  void accountLevelObject_shouldReturnFalse_whenObjNameIsWildcard() {
    PlaybookPattern pattern =
        new PlaybookPattern(Optional.empty(), Optional.empty(), Optional.of("*"));

    assertFalse(PlaybookPatternMatchers.accountLevelObject("ANY_OBJ").apply(pattern));
  }

  @Test
  void accountLevelObject_shouldReturnFalse_whenDbNameIsNotNull() {
    PlaybookPattern pattern =
        new PlaybookPattern(Optional.of("DB"), Optional.empty(), Optional.of("OBJ"));

    assertFalse(PlaybookPatternMatchers.accountLevelObject("OBJ").apply(pattern));
  }

  @Test
  void accountLevelObject_shouldReturnFalse_whenSchemaNameIsNotNull() {
    PlaybookPattern pattern =
        new PlaybookPattern(Optional.empty(), Optional.of("SCHEMA"), Optional.of("OBJ"));

    assertFalse(PlaybookPatternMatchers.accountLevelObject("OBJ").apply(pattern));
  }

  @Test
  void accountLevelObject_shouldReturnFalse_whenObjNameIsNull() {
    PlaybookPattern pattern =
        new PlaybookPattern(Optional.empty(), Optional.empty(), Optional.empty());

    assertFalse(PlaybookPatternMatchers.accountLevelObject("OBJ").apply(pattern));
  }

  @Test
  void database_shouldMatch_whenDatabaseNameMatchesCaseInsensitively() {
    PlaybookPattern pattern =
        new PlaybookPattern(Optional.of("TEST_DB"), Optional.empty(), Optional.empty());

    assertTrue(
        PlaybookPatternMatchers.database("TEST_DB").apply(pattern),
        "Should match exact database name");
    assertTrue(
        PlaybookPatternMatchers.database("test_db").apply(pattern),
        "Should match case-insensitively");
  }

  @Test
  void database_shouldReturnFalse_whenDatabaseIsWildcard() {
    PlaybookPattern pattern =
        new PlaybookPattern(Optional.of("*"), Optional.empty(), Optional.empty());

    assertFalse(PlaybookPatternMatchers.database("ANY_DB").apply(pattern));
  }

  @Test
  void database_shouldReturnFalse_whenDatabaseIsNull() {
    PlaybookPattern pattern =
        new PlaybookPattern(Optional.empty(), Optional.empty(), Optional.empty());

    assertFalse(PlaybookPatternMatchers.database("ANY_DB").apply(pattern));
  }

  @Test
  void database_shouldReturnFalse_whenDatabaseDoesNotMatch() {
    PlaybookPattern pattern =
        new PlaybookPattern(Optional.of("TEST_DB"), Optional.empty(), Optional.empty());

    assertFalse(PlaybookPatternMatchers.database("OTHER_DB").apply(pattern));
  }

  @Test
  void schema_shouldReturnTrue_whenSchemaIsNull() {
    PlaybookPattern pattern =
        new PlaybookPattern(Optional.of("DB"), Optional.empty(), Optional.empty());

    assertTrue(PlaybookPatternMatchers.schema("ANY_SCHEMA").apply(pattern));
  }

  @Test
  void schema_shouldReturnTrue_whenSchemaIsWildcard() {
    PlaybookPattern pattern =
        new PlaybookPattern(Optional.of("DB"), Optional.of("*"), Optional.empty());

    assertTrue(PlaybookPatternMatchers.schema("ANY_SCHEMA").apply(pattern));
  }

  @Test
  void schema_shouldMatch_whenSchemaNameMatchesCaseInsensitively() {
    PlaybookPattern pattern =
        new PlaybookPattern(Optional.of("DB"), Optional.of("TEST_SCHEMA"), Optional.empty());

    assertTrue(
        PlaybookPatternMatchers.schema("TEST_SCHEMA").apply(pattern),
        "Should match exact schema name");
    assertTrue(
        PlaybookPatternMatchers.schema("test_schema").apply(pattern),
        "Should match case-insensitively");
  }

  @Test
  void schema_shouldNotMatch_whenSchemaNameDiffers() {
    PlaybookPattern pattern =
        new PlaybookPattern(Optional.of("DB"), Optional.of("TEST_SCHEMA"), Optional.empty());

    assertFalse(
        PlaybookPatternMatchers.schema("OTHER_SCHEMA").apply(pattern),
        "Should not match when schema names differ");
  }

  @Test
  void object_shouldReturnTrue_whenObjectIsNull() {
    PlaybookPattern pattern =
        new PlaybookPattern(Optional.of("DB"), Optional.of("SCHEMA"), Optional.empty());

    assertTrue(PlaybookPatternMatchers.object("ANY_OBJ").apply(pattern));
  }

  @Test
  void object_shouldReturnTrue_whenObjectIsWildcard() {
    PlaybookPattern pattern =
        new PlaybookPattern(Optional.of("DB"), Optional.of("SCHEMA"), Optional.of("*"));

    assertTrue(PlaybookPatternMatchers.object("ANY_OBJ").apply(pattern));
  }

  @Test
  void object_shouldMatch_whenObjectNameMatchesCaseInsensitively() {
    PlaybookPattern pattern =
        new PlaybookPattern(Optional.of("DB"), Optional.of("SCHEMA"), Optional.of("TEST_OBJ"));

    assertTrue(
        PlaybookPatternMatchers.object("TEST_OBJ").apply(pattern),
        "Should match exact object name");
    assertTrue(
        PlaybookPatternMatchers.object("test_obj").apply(pattern),
        "Should match case-insensitively");
  }

  @Test
  void object_shouldNotMatch_whenObjectNameDiffers() {
    PlaybookPattern pattern =
        new PlaybookPattern(Optional.of("DB"), Optional.of("SCHEMA"), Optional.of("TEST_OBJ"));

    assertFalse(
        PlaybookPatternMatchers.object("OTHER_OBJ").apply(pattern),
        "Should not match when object names differ");
  }

  @Test
  void database_shouldNotMatch_whenPatternDatabaseIsEmptyString() {
    // Edge case: Empty string in pattern should not match (different from null/wildcard)
    PlaybookPattern pattern =
        new PlaybookPattern(Optional.of(""), Optional.empty(), Optional.empty());

    assertFalse(
        PlaybookPatternMatchers.database("ANY_DB").apply(pattern),
        "Empty string pattern should not match any database");
  }

  @Test
  void accountLevelObject_shouldReturnFalse_whenObjectNameDoesNotMatch() {
    // Critical: Test the ObjectName.equalObjectName branch when it returns false
    PlaybookPattern pattern =
        new PlaybookPattern(Optional.empty(), Optional.empty(), Optional.of("OBJ_NAME"));

    assertFalse(
        PlaybookPatternMatchers.accountLevelObject("DIFFERENT_OBJ").apply(pattern),
        "Should return false when object name doesn't match");
  }

  @Test
  void accountLevelObject_shouldReturnFalse_whenObjNameIsEmptyString() {
    // Critical: Test when objName is present but empty string (after filtering)
    PlaybookPattern pattern =
        new PlaybookPattern(Optional.empty(), Optional.empty(), Optional.of(""));

    assertFalse(
        PlaybookPatternMatchers.accountLevelObject("ANY_OBJ").apply(pattern),
        "Should return false when objName is empty string");
  }

  @Test
  void accountLevelObject_shouldReturnFalse_whenObjNameMatchesButDbNameIsPresent() {
    // Critical: Test when objName matches but dbName is present (should fail first condition)
    PlaybookPattern pattern =
        new PlaybookPattern(Optional.of("DB"), Optional.empty(), Optional.of("OBJ"));

    assertFalse(
        PlaybookPatternMatchers.accountLevelObject("OBJ").apply(pattern),
        "Should return false when dbName is present even if objName matches");
  }

  @Test
  void accountLevelObject_shouldReturnFalse_whenObjNameMatchesButSchNameIsPresent() {
    // Critical: Test when objName matches but schName is present (should fail second condition)
    PlaybookPattern pattern =
        new PlaybookPattern(Optional.empty(), Optional.of("SCHEMA"), Optional.of("OBJ"));

    assertFalse(
        PlaybookPatternMatchers.accountLevelObject("OBJ").apply(pattern),
        "Should return false when schName is present even if objName matches");
  }
}
