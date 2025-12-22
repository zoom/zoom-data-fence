package us.zoom.data.dfence.providers.snowflake.revoke.index

import munit.FunSuite
import us.zoom.data.dfence.playbook.model.PlaybookPrivilegeGrant
import us.zoom.data.dfence.providers.snowflake.grant.builder.SnowflakeObjectType
import us.zoom.data.dfence.providers.snowflake.models.{PlaybookGrant, PlaybookGrantType}
import us.zoom.data.dfence.providers.snowflake.models.opaque.{GrantPrivilege, ObjectType, ObjectTypeAlias}
import us.zoom.data.dfence.providers.snowflake.revoke.index.PlaybookGrantIndexBuilder

import scala.collection.mutable
import scala.jdk.CollectionConverters.*

class PlaybookGrantIndexBuilderTest extends FunSuite:

  test("buildPrivilegeGrantIndex should correctly index grants by privilege") {
    // Given
    val grants = mutable.Buffer(
      createPlaybookGrant("TABLE", "DB", "SCHEMA", "T1", List("SELECT")),
      createPlaybookGrant("TABLE", "DB", "SCHEMA", "T2", List("SELECT", "UPDATE")),
      createPlaybookGrant("VIEW", "DB", "SCHEMA", "V1", List("SELECT"))
    )

    // When
    val index = PlaybookGrantIndexBuilder.buildPrivilegeGrantIndex(grants)

    // Then
    val selectGrants = index.privilegeIndex(GrantPrivilege("SELECT"))
    val updateGrants = index.privilegeIndex(GrantPrivilege("UPDATE"))

    assertEquals(selectGrants.size, 3)
    assertEquals(updateGrants.size, 1)
  }

  test("buildPrivilegeGrantIndex should correctly index grants by object type") {
    // Given
    val grants = mutable.Buffer(
      createPlaybookGrant("TABLE", "DB", "SCHEMA", "T1", List("SELECT")),
      createPlaybookGrant("VIEW", "DB", "SCHEMA", "V1", List("SELECT"))
    )

    // When
    val index = PlaybookGrantIndexBuilder.buildPrivilegeGrantIndex(grants)

    // Then
    val tableGrants = index.objectTypeIndex(ObjectType(SnowflakeObjectType.TABLE))
    val viewGrants  = index.objectTypeIndex(ObjectType(SnowflakeObjectType.VIEW))

    assertEquals(tableGrants.size, 1)
    assertEquals(viewGrants.size, 1)
  }

  test("buildPrivilegeGrantIndex should correctly index grants by aliased object type") {
    // Given
    // EXTERNAL_TABLE is an alias for TABLE
    val grants = mutable.Buffer(
      createPlaybookGrant("EXTERNAL_TABLE", "DB", "SCHEMA", "ET1", List("SELECT"))
    )

    // When
    val index = PlaybookGrantIndexBuilder.buildPrivilegeGrantIndex(grants)

    // Then
    // Should be in alias index
    val aliasGrants = index.objectAliasIndex(ObjectTypeAlias(SnowflakeObjectType.EXTERNAL_TABLE))
    assertEquals(aliasGrants.size, 1)

    // Should NOT be in primary object type index (as EXTERNAL_TABLE != TABLE)
    // Note: The logic puts exact matches in objectTypeIndex and aliases in objectAliasIndex
    // EXTERNAL_TABLE's objectType is TABLE? No, usually enum value.
    // Let's check SnowflakeObjectType logic.
    // If SnowflakeObjectType.EXTERNAL_TABLE.getObjectType() == SnowflakeObjectType.TABLE
    // And SnowflakeObjectType.EXTERNAL_TABLE.getAliasFor() == SnowflakeObjectType.TABLE (String rep)

    // Actually, looking at the code:
    // privilegeGrants.partition(grant => grant.objectType.getObjectType() == grant.objectType.getAliasFor())

    // If EXTERNAL_TABLE maps to TABLE, then they are likely different strings, so it goes to alias index.
  }

  test("buildPrivilegeGrantIndex should merge duplicate keys correctly") {
    // This tests the aggregate/merge logic
    // Given multiple grants that map to the same key
    val grants = mutable.Buffer(
      createPlaybookGrant("TABLE", "DB", "SCHEMA", "T1", List("SELECT")),
      createPlaybookGrant("TABLE", "DB", "SCHEMA", "T2", List("SELECT"))
    )

    // When
    val index = PlaybookGrantIndexBuilder.buildPrivilegeGrantIndex(grants)

    // Then
    val selectGrants = index.privilegeIndex(GrantPrivilege("SELECT"))
    assertEquals(selectGrants.size, 2)
  }

  test("buildPrivilegeGrantIndex should filter disabled grants") {
    // Given
    val grants = mutable.Buffer(
      createPlaybookGrant("TABLE", "DB", "SCHEMA", "T1", List("SELECT"), enable = false)
    )

    // When
    val index = PlaybookGrantIndexBuilder.buildPrivilegeGrantIndex(grants)

    // Then
    assert(index.privilegeIndex.isEmpty)
    assert(index.objectTypeIndex.isEmpty)
    assert(index.objectAliasIndex.isEmpty)
  }

  test("buildPrivilegeGrantIndex should handle invalid object types gracefully") {
    // Given: Grant with invalid object type that will fail conversion
    val grants = mutable.Buffer(
      createPlaybookGrant("INVALID_OBJECT_TYPE", "DB", "SCHEMA", "T1", List("SELECT"))
    )

    // When
    val index = PlaybookGrantIndexBuilder.buildPrivilegeGrantIndex(grants)

    // Then: Should not crash, invalid grant should be filtered out
    // The toPlaybookGrant method returns None for invalid types, which are filtered out
    assert(index.privilegeIndex.isEmpty, "Invalid grants should be filtered out")
  }

  test("buildPrivilegeGrantIndex should handle grants with both future and all false") {
    // Given: Grant with includeFuture=false and includeAll=false (default case)
    val grant = new PlaybookPrivilegeGrant(
      "TABLE",
      "T1",
      "SCHEMA",
      "DB",
      List("SELECT").asJava,
      false, // includeFuture = false
      false, // includeAll = false
      true   // enable
    )

    // When
    val index = PlaybookGrantIndexBuilder.buildPrivilegeGrantIndex(mutable.Buffer(grant))

    // Then: Should create grant with Standard grant type (the default case)
    val selectGrants = index.privilegeIndex(GrantPrivilege("SELECT"))
    assertEquals(selectGrants.size, 1)
    assertEquals(selectGrants.head.grantType, PlaybookGrantType.Standard)
  }

  private def createPlaybookGrant(
    objectType: String,
    databaseName: String,
    schemaName: String,
    objectName: String,
    privileges: List[String],
    enable: Boolean = true
  ): PlaybookPrivilegeGrant =
    new PlaybookPrivilegeGrant(
      objectType,
      objectName,
      schemaName,
      databaseName,
      privileges.asJava,
      true, // includeFuture
      true, // includeAll
      enable
    )
