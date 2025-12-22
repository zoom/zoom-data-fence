package us.zoom.data.dfence.providers.snowflake.revoke

import munit.FunSuite
import us.zoom.data.dfence.playbook.model.PlaybookPrivilegeGrant
import us.zoom.data.dfence.providers.snowflake.grant.builder.SnowflakeGrantBuilder
import us.zoom.data.dfence.providers.snowflake.models.SnowflakeGrantModel
import us.zoom.data.dfence.providers.snowflake.revoke.SnowflakeRevokeGrantsCompiler

import java.util.{HashMap, List as JList, Map as JMap}
import scala.jdk.CollectionConverters.*

class SnowflakeRevokeGrantsCompilerTest extends FunSuite:

  private val compiler = SnowflakeRevokeGrantsCompiler()

  test("compileRevokeGrants when grant matches playbook should not revoke") {
    // Given
    val playbookGrant =
      createPlaybookGrant("TABLE", "TEST_DB", "TEST_SCHEMA", "TEST_TABLE", List("SELECT"))
    val currentGrant  =
      createGrant("SELECT", "TABLE", "TEST_DB.TEST_SCHEMA.TEST_TABLE", "ROLE", "TEST_ROLE")
    val currentGrants = createCurrentGrants(currentGrant)

    // When
    val actualRevokes = compiler.compileRevokeGrants(JList.of(playbookGrant), currentGrants)

    // Then
    assert(actualRevokes.isEmpty)
  }

  test("compileRevokeGrants when grant not in playbook should revoke") {
    // Given
    val playbookGrant =
      createPlaybookGrant("TABLE", "TEST_DB", "TEST_SCHEMA", "TEST_TABLE", List("SELECT"))
    val currentGrant  =
      createGrant("UPDATE", "TABLE", "TEST_DB.TEST_SCHEMA.TEST_TABLE", "ROLE", "TEST_ROLE")
    val currentGrants = createCurrentGrants(currentGrant)

    // When
    val actualRevokes = compiler.compileRevokeGrants(JList.of(playbookGrant), currentGrants)

    // Then
    val privileges = actualRevokes.asScala.map(_.getGrant.privilege()).toList
    assertEquals(privileges, List("UPDATE"))
  }

  test("compileRevokeGrants when wildcard matches should not revoke") {
    // Given
    val playbookGrant =
      createPlaybookGrant("TABLE", "TEST_DB", "TEST_SCHEMA", "*", List("SELECT"))
    val currentGrant  =
      createGrant("SELECT", "TABLE", "TEST_DB.TEST_SCHEMA.ANY_TABLE", "ROLE", "TEST_ROLE")
    val currentGrants = createCurrentGrants(currentGrant)

    // When
    val actualRevokes = compiler.compileRevokeGrants(JList.of(playbookGrant), currentGrants)

    // Then
    assert(actualRevokes.isEmpty)
  }

  test("compileRevokeGrants when alias matches should not revoke") {
    // Given: With the new implementation, alias matching requires the playbook grant
    // to have the same object type as the current grant, or be indexed correctly.
    // For EXTERNAL_TABLE to match, we need an EXTERNAL_TABLE playbook grant
    // (since the filter requires grant.objectType == grantObjectType)
    val playbookGrant =
      createPlaybookGrant("EXTERNAL_TABLE", "TEST_DB", "TEST_SCHEMA", "TEST_TABLE", List("SELECT"))
    val currentGrant  =
      createGrant("SELECT", "EXTERNAL_TABLE", "TEST_DB.TEST_SCHEMA.TEST_TABLE", "ROLE", "TEST_ROLE")
    val currentGrants = createCurrentGrants(currentGrant)

    // When
    val actualRevokes = compiler.compileRevokeGrants(JList.of(playbookGrant), currentGrants)

    // Then
    assert(actualRevokes.isEmpty)
  }

  test("compileRevokeGrants when future grant compatible should not revoke") {
    // Given
    val playbookGrant = createPlaybookGrant(
      "TABLE",
      "TEST_DB",
      "TEST_SCHEMA",
      "*",
      List("SELECT"),
      includeFuture = true,
      includeAll = true,
      enable = true
    )
    val currentGrant  =
      createFutureGrant("SELECT", "TABLE", "TEST_DB.TEST_SCHEMA", "ROLE", "TEST_ROLE")
    val currentGrants = createCurrentGrants(currentGrant)

    // When
    val actualRevokes = compiler.compileRevokeGrants(JList.of(playbookGrant), currentGrants)

    // Then
    assert(actualRevokes.isEmpty)
  }

  test("compileRevokeGrants when future grant not compatible should revoke") {
    // Given
    val playbookGrant = createPlaybookGrant(
      "TABLE",
      "TEST_DB",
      "TEST_SCHEMA",
      "*",
      List("SELECT"),
      includeFuture = false,
      includeAll = true,
      enable = true
    )
    val currentGrant  =
      createFutureGrant("SELECT", "TABLE", "TEST_DB.TEST_SCHEMA", "ROLE", "TEST_ROLE")
    val currentGrants = createCurrentGrants(currentGrant)

    // When
    val actualRevokes = compiler.compileRevokeGrants(JList.of(playbookGrant), currentGrants)

    // Then
    assertEquals(actualRevokes.size(), 1)
    val revokedGrant = actualRevokes.get(0).getGrant
    assertEquals(revokedGrant.privilege(), "SELECT")
    assertEquals(revokedGrant.grantedOn(), "TABLE")
    assertEquals(revokedGrant.name(), "TEST_DB.TEST_SCHEMA")
    assertEquals(revokedGrant.grantedTo(), "ROLE")
    assertEquals(revokedGrant.granteeName(), "TEST_ROLE")
    assertEquals(revokedGrant.future().booleanValue(), true)
    assertEquals(revokedGrant.all().booleanValue(), false)
  }

  test("compileRevokeGrants when all grant compatible should not revoke") {
    // Given
    val playbookGrant = createPlaybookGrant(
      "TABLE",
      "TEST_DB",
      "TEST_SCHEMA",
      "*",
      List("SELECT"),
      includeFuture = true,
      includeAll = true,
      enable = true
    )
    val currentGrant  = createAllGrant("SELECT", "TABLE", "TEST_DB.TEST_SCHEMA", "ROLE", "TEST_ROLE")
    val currentGrants = createCurrentGrants(currentGrant)

    // When
    val actualRevokes = compiler.compileRevokeGrants(JList.of(playbookGrant), currentGrants)

    // Then
    assert(actualRevokes.isEmpty)
  }

  test("compileRevokeGrants when role grant matches should not revoke") {
    // Given
    val playbookGrant = createPlaybookGrantForRole("TEST_ROLE", List("USAGE"))
    val currentGrant  = createGrant("USAGE", "ROLE", "TEST_ROLE", "ROLE", "ANOTHER_ROLE")
    val currentGrants = createCurrentGrants(currentGrant)

    // When
    val actualRevokes = compiler.compileRevokeGrants(JList.of(playbookGrant), currentGrants)

    // Then
    assert(actualRevokes.isEmpty)
  }

  test("compileRevokeGrants when database grant matches should not revoke") {
    // Given
    val playbookGrant = createPlaybookGrant("DATABASE", "TEST_DB", null, null, List("USAGE"))
    val currentGrant  = createGrant("USAGE", "DATABASE", "TEST_DB", "ROLE", "TEST_ROLE")
    val currentGrants = createCurrentGrants(currentGrant)

    // When
    val actualRevokes = compiler.compileRevokeGrants(JList.of(playbookGrant), currentGrants)

    // Then
    assert(actualRevokes.isEmpty)
  }

  test("compileRevokeGrants when schema grant matches should not revoke") {
    // Given
    val playbookGrant =
      createPlaybookGrant("SCHEMA", "TEST_DB", "TEST_SCHEMA", null, List("USAGE"))
    val currentGrant  = createGrant("USAGE", "SCHEMA", "TEST_DB.TEST_SCHEMA", "ROLE", "TEST_ROLE")
    val currentGrants = createCurrentGrants(currentGrant)

    // When
    val actualRevokes = compiler.compileRevokeGrants(JList.of(playbookGrant), currentGrants)

    // Then
    assert(actualRevokes.isEmpty)
  }

  test("compileRevokeGrants when empty playbook should revoke all grants") {
    // Given
    val currentGrant  =
      createGrant("SELECT", "TABLE", "TEST_DB.TEST_SCHEMA.TEST_TABLE", "ROLE", "TEST_ROLE")
    val currentGrants = createCurrentGrants(currentGrant)

    // When
    val actualRevokes = compiler.compileRevokeGrants(JList.of(), currentGrants)

    // Then
    assertEquals(actualRevokes.size(), 1)
    val revokedGrant = actualRevokes.get(0).getGrant
    assertEquals(revokedGrant.privilege(), "SELECT")
    assertEquals(revokedGrant.grantedOn(), "TABLE")
    assertEquals(revokedGrant.name(), "TEST_DB.TEST_SCHEMA.TEST_TABLE")
    assertEquals(revokedGrant.grantedTo(), "ROLE")
    assertEquals(revokedGrant.granteeName(), "TEST_ROLE")
  }

  test("compileRevokeGrants when empty current grants should return empty") {
    // Given
    val playbookGrant =
      createPlaybookGrant("TABLE", "TEST_DB", "TEST_SCHEMA", "TEST_TABLE", List("SELECT"))
    val currentGrants = new HashMap[String, SnowflakeGrantBuilder]()

    // When
    val actualRevokes = compiler.compileRevokeGrants(JList.of(playbookGrant), currentGrants)

    // Then
    assert(actualRevokes.isEmpty)
  }

  test("compileRevokeGrants when disabled playbook grant should revoke") {
    // Given
    val playbookGrant = createPlaybookGrant(
      "TABLE",
      "TEST_DB",
      "TEST_SCHEMA",
      "TEST_TABLE",
      List("SELECT"),
      includeFuture = true,
      includeAll = true,
      enable = false
    )
    val currentGrant  =
      createGrant("SELECT", "TABLE", "TEST_DB.TEST_SCHEMA.TEST_TABLE", "ROLE", "TEST_ROLE")
    val currentGrants = createCurrentGrants(currentGrant)

    // When
    val actualRevokes = compiler.compileRevokeGrants(JList.of(playbookGrant), currentGrants)

    // Then
    assertEquals(actualRevokes.size(), 1)
    val revokedGrant = actualRevokes.get(0).getGrant
    assertEquals(revokedGrant.privilege(), "SELECT")
    assertEquals(revokedGrant.grantedOn(), "TABLE")
    assertEquals(revokedGrant.name(), "TEST_DB.TEST_SCHEMA.TEST_TABLE")
    assertEquals(revokedGrant.grantedTo(), "ROLE")
    assertEquals(revokedGrant.granteeName(), "TEST_ROLE")
  }

  test("compileRevokeGrants when multiple grants should have mixed results") {
    // Given
    val playbookGrant =
      createPlaybookGrant("TABLE", "TEST_DB", "TEST_SCHEMA", "*", List("SELECT"))
    val allowedGrant  =
      createGrant("SELECT", "TABLE", "TEST_DB.TEST_SCHEMA.TABLE1", "ROLE", "TEST_ROLE")
    val revokedGrant1 =
      createGrant("UPDATE", "TABLE", "TEST_DB.TEST_SCHEMA.TABLE2", "ROLE", "TEST_ROLE")
    val revokedGrant2 =
      createGrant("DELETE", "TABLE", "TEST_DB.TEST_SCHEMA.TABLE3", "ROLE", "TEST_ROLE")
    val currentGrants = createCurrentGrants(allowedGrant, revokedGrant1, revokedGrant2)

    // When
    val actualRevokes = compiler.compileRevokeGrants(JList.of(playbookGrant), currentGrants)

    // Then
    val privileges = actualRevokes.asScala.map(_.getGrant.privilege()).toList
    assert(privileges.contains("UPDATE"))
    assert(privileges.contains("DELETE"))
    assertEquals(privileges.size, 2)
  }

  test("compileRevokeGrants when all grant not compatible should revoke") {
    // Given
    val playbookGrant = createPlaybookGrant(
      "TABLE",
      "TEST_DB",
      "TEST_SCHEMA",
      "*",
      List("SELECT"),
      includeFuture = true,
      includeAll = false,
      enable = true
    )
    val currentGrant  = createAllGrant("SELECT", "TABLE", "TEST_DB.TEST_SCHEMA", "ROLE", "TEST_ROLE")
    val currentGrants = createCurrentGrants(currentGrant)

    // When
    val actualRevokes = compiler.compileRevokeGrants(JList.of(playbookGrant), currentGrants)

    // Then
    assertEquals(actualRevokes.size(), 1)
    val revokedGrant = actualRevokes.get(0).getGrant
    assertEquals(revokedGrant.privilege(), "SELECT")
    assertEquals(revokedGrant.grantedOn(), "TABLE")
    assertEquals(revokedGrant.name(), "TEST_DB.TEST_SCHEMA")
    assertEquals(revokedGrant.grantedTo(), "ROLE")
    assertEquals(revokedGrant.granteeName(), "TEST_ROLE")
    assertEquals(revokedGrant.future().booleanValue(), false)
    assertEquals(revokedGrant.all().booleanValue(), true)
  }

  test("compileRevokeGrants when schema wildcard matches should not revoke") {
    // Given
    val playbookGrant = createPlaybookGrant("SCHEMA", "TEST_DB", "*", null, List("USAGE"))
    val currentGrant  = createGrant("USAGE", "SCHEMA", "TEST_DB.ANY_SCHEMA", "ROLE", "TEST_ROLE")
    val currentGrants = createCurrentGrants(currentGrant)

    // When
    val actualRevokes = compiler.compileRevokeGrants(JList.of(playbookGrant), currentGrants)

    // Then
    assert(actualRevokes.isEmpty)
  }

  test("compileRevokeGrants when different object type should revoke") {
    // Given
    val playbookGrant =
      createPlaybookGrant("TABLE", "TEST_DB", "TEST_SCHEMA", "TEST_TABLE", List("SELECT"))
    val currentGrant  =
      createGrant("SELECT", "VIEW", "TEST_DB.TEST_SCHEMA.TEST_TABLE", "ROLE", "TEST_ROLE")
    val currentGrants = createCurrentGrants(currentGrant)

    // When
    val actualRevokes = compiler.compileRevokeGrants(JList.of(playbookGrant), currentGrants)

    // Then
    assertEquals(actualRevokes.size(), 1)
    val revokedGrant = actualRevokes.get(0).getGrant
    assertEquals(revokedGrant.privilege(), "SELECT")
    assertEquals(revokedGrant.grantedOn(), "VIEW")
    assertEquals(revokedGrant.name(), "TEST_DB.TEST_SCHEMA.TEST_TABLE")
    assertEquals(revokedGrant.grantedTo(), "ROLE")
    assertEquals(revokedGrant.granteeName(), "TEST_ROLE")
  }

  test("compileRevokeGrants when empty privileges list should not index privilege") {
    // Given
    val playbookGrant = new PlaybookPrivilegeGrant(
      "TABLE",
      "TEST_TABLE",
      "TEST_SCHEMA",
      "TEST_DB",
      JList.of(),
      true,
      true,
      true
    )
    val currentGrant  =
      createGrant("SELECT", "TABLE", "TEST_DB.TEST_SCHEMA.TEST_TABLE", "ROLE", "TEST_ROLE")
    val currentGrants = createCurrentGrants(currentGrant)

    // When
    val actualRevokes = compiler.compileRevokeGrants(JList.of(playbookGrant), currentGrants)

    // Then
    assertEquals(actualRevokes.size(), 1)
    val revokedGrant = actualRevokes.get(0).getGrant
    assertEquals(revokedGrant.privilege(), "SELECT")
    assertEquals(revokedGrant.grantedOn(), "TABLE")
    assertEquals(revokedGrant.name(), "TEST_DB.TEST_SCHEMA.TEST_TABLE")
    assertEquals(revokedGrant.grantedTo(), "ROLE")
    assertEquals(revokedGrant.granteeName(), "TEST_ROLE")
  }

  test("compileRevokeGrants when invalid object type in playbook should still work") {
    // Given
    val playbookGrant = new PlaybookPrivilegeGrant(
      "INVALID_TYPE",
      "TEST_TABLE",
      "TEST_SCHEMA",
      "TEST_DB",
      JList.of("SELECT"),
      true,
      true,
      true
    )
    val currentGrant  =
      createGrant("SELECT", "TABLE", "TEST_DB.TEST_SCHEMA.TEST_TABLE", "ROLE", "TEST_ROLE")
    val currentGrants = createCurrentGrants(currentGrant)

    // When
    val actualRevokes = compiler.compileRevokeGrants(JList.of(playbookGrant), currentGrants)

    // Then
    assertEquals(actualRevokes.size(), 1)
    val revokedGrant = actualRevokes.get(0).getGrant
    assertEquals(revokedGrant.privilege(), "SELECT")
    assertEquals(revokedGrant.grantedOn(), "TABLE")
    assertEquals(revokedGrant.name(), "TEST_DB.TEST_SCHEMA.TEST_TABLE")
    assertEquals(revokedGrant.grantedTo(), "ROLE")
    assertEquals(revokedGrant.granteeName(), "TEST_ROLE")
  }

  test("compileRevokeGrants when invalid object type in grant cannot create builder") {
    // Given
    val playbookGrant =
      createPlaybookGrant("TABLE", "TEST_DB", "TEST_SCHEMA", "TEST_TABLE", List("SELECT"))
    val invalidGrant  = new SnowflakeGrantModel(
      "SELECT",
      "INVALID_TYPE",
      "TEST_DB.TEST_SCHEMA.TEST_TABLE",
      "ROLE",
      "TEST_ROLE",
      false,
      false,
      false
    )
    val validGrant    =
      createGrant("UPDATE", "TABLE", "TEST_DB.TEST_SCHEMA.TEST_TABLE", "ROLE", "TEST_ROLE")
    val currentGrants = createCurrentGrants(invalidGrant, validGrant)

    // When
    val actualRevokes = compiler.compileRevokeGrants(JList.of(playbookGrant), currentGrants)

    // Then
    assertEquals(currentGrants.values().size(), 1)
    assertEquals(actualRevokes.size(), 1)
    val revokedGrant = actualRevokes.get(0).getGrant
    assertEquals(revokedGrant.privilege(), "UPDATE")
    assertEquals(revokedGrant.grantedOn(), "TABLE")
  }

  test("compileRevokeGrants when database and schema wildcards should not revoke") {
    // Given
    val playbookGrant = createPlaybookGrant("TABLE", "TEST_DB", "*", "*", List("SELECT"))
    val currentGrant  =
      createGrant("SELECT", "TABLE", "TEST_DB.ANY_SCHEMA.ANY_TABLE", "ROLE", "TEST_ROLE")
    val currentGrants = createCurrentGrants(currentGrant)

    // When
    val actualRevokes = compiler.compileRevokeGrants(JList.of(playbookGrant), currentGrants)

    // Then
    assert(actualRevokes.isEmpty)
  }

  test(
    "compileRevokeGrants when database and schema wildcards different schema should not revoke"
  ) {
    // Given
    val playbookGrant = createPlaybookGrant("TABLE", "TEST_DB", "*", "*", List("SELECT"))
    val currentGrant  = createGrant(
      "SELECT",
      "TABLE",
      "TEST_DB.DIFFERENT_SCHEMA.DIFFERENT_TABLE",
      "ROLE",
      "TEST_ROLE"
    )
    val currentGrants = createCurrentGrants(currentGrant)

    // When
    val actualRevokes = compiler.compileRevokeGrants(JList.of(playbookGrant), currentGrants)

    // Then
    assert(actualRevokes.isEmpty)
  }

  test(
    "compileRevokeGrants when database and schema wildcards wrong database should revoke"
  ) {
    // Given
    val playbookGrant = createPlaybookGrant("TABLE", "TEST_DB", "*", "*", List("SELECT"))
    val currentGrant  =
      createGrant("SELECT", "TABLE", "OTHER_DB.ANY_SCHEMA.ANY_TABLE", "ROLE", "TEST_ROLE")
    val currentGrants = createCurrentGrants(currentGrant)

    // When
    val actualRevokes = compiler.compileRevokeGrants(JList.of(playbookGrant), currentGrants)

    // Then
    assertEquals(actualRevokes.size(), 1)
    val revokedGrant = actualRevokes.get(0).getGrant
    assertEquals(revokedGrant.privilege(), "SELECT")
    assertEquals(revokedGrant.grantedOn(), "TABLE")
    assertEquals(revokedGrant.name(), "OTHER_DB.ANY_SCHEMA.ANY_TABLE")
    assertEquals(revokedGrant.grantedTo(), "ROLE")
    assertEquals(revokedGrant.granteeName(), "TEST_ROLE")
  }

  test(
    "compileRevokeGrants when database level playbook matches database level future grant should not revoke"
  ) {
    // Given
    val playbookGrant =
      createPlaybookGrant("TABLE", "TEST_DB", null, "*", List("SELECT"), true, true, true)
    val futureGrant   = createFutureGrant("SELECT", "TABLE", "TEST_DB", "ROLE", "TEST_ROLE")
    val currentGrants = createCurrentGrants(futureGrant)

    // When
    val actualRevokes = compiler.compileRevokeGrants(JList.of(playbookGrant), currentGrants)

    // Then
    assert(actualRevokes.isEmpty)
  }

  test(
    "compileRevokeGrants when database level playbook matches database level all grant should not revoke"
  ) {
    // Given
    val playbookGrant =
      createPlaybookGrant("TABLE", "TEST_DB", null, "*", List("SELECT"), true, true, true)
    val allGrant      = createAllGrant("SELECT", "TABLE", "TEST_DB", "ROLE", "TEST_ROLE")
    val currentGrants = createCurrentGrants(allGrant)

    // When
    val actualRevokes = compiler.compileRevokeGrants(JList.of(playbookGrant), currentGrants)

    // Then
    assert(actualRevokes.isEmpty)
  }

  test(
    "compileRevokeGrants when database level playbook does not match wrong database should revoke"
  ) {
    // Given
    val playbookGrant =
      createPlaybookGrant("TABLE", "TEST_DB", null, "*", List("SELECT"), true, true, true)
    val futureGrant   = createFutureGrant("SELECT", "TABLE", "OTHER_DB", "ROLE", "TEST_ROLE")
    val currentGrants = createCurrentGrants(futureGrant)

    // When
    val actualRevokes = compiler.compileRevokeGrants(JList.of(playbookGrant), currentGrants)

    // Then
    assertEquals(actualRevokes.size(), 1)
    val revokedGrant = actualRevokes.get(0).getGrant
    assertEquals(revokedGrant.privilege(), "SELECT")
    assertEquals(revokedGrant.grantedOn(), "TABLE")
    assertEquals(revokedGrant.name(), "OTHER_DB")
    assertEquals(revokedGrant.future().booleanValue(), true)
  }

  test(
    "compileRevokeGrants when playbook with wildcard schema matches database level future grant should not revoke"
  ) {
    // Given
    val playbookGrant =
      createPlaybookGrant("TABLE", "TEST_DB", "*", "*", List("SELECT"), true, true, true)
    val futureGrant   = createFutureGrant("SELECT", "TABLE", "TEST_DB", "ROLE", "TEST_ROLE")
    val currentGrants = createCurrentGrants(futureGrant)

    // When
    val actualRevokes = compiler.compileRevokeGrants(JList.of(playbookGrant), currentGrants)

    // Then
    assert(actualRevokes.isEmpty)
  }

  test(
    "compileRevokeGrants when playbook with wildcard schema matches database level all grant should not revoke"
  ) {
    // Given
    val playbookGrant =
      createPlaybookGrant("TABLE", "TEST_DB", "*", "*", List("SELECT"), true, true, true)
    val allGrant      = createAllGrant("SELECT", "TABLE", "TEST_DB", "ROLE", "TEST_ROLE")
    val currentGrants = createCurrentGrants(allGrant)

    // When
    val actualRevokes = compiler.compileRevokeGrants(JList.of(playbookGrant), currentGrants)

    // Then
    assert(actualRevokes.isEmpty)
  }

  test(
    "compileRevokeGrants when database future grant matches playbook should not revoke"
  ) {
    // Given
    val playbookGrant =
      createPlaybookGrant("DATABASE", "TEST_DB", null, null, List("USAGE"), true, true, true)
    val futureGrant   = createFutureGrant("USAGE", "DATABASE", "TEST_DB", "ROLE", "TEST_ROLE")
    val currentGrants = createCurrentGrants(futureGrant)

    // When
    val actualRevokes = compiler.compileRevokeGrants(JList.of(playbookGrant), currentGrants)

    // Then
    assert(actualRevokes.isEmpty)
  }

  test(
    "compileRevokeGrants when database future grant does not match playbook should revoke"
  ) {
    // Given
    val playbookGrant =
      createPlaybookGrant("DATABASE", "TEST_DB", null, null, List("USAGE"), true, true, true)
    val futureGrant   = createFutureGrant("USAGE", "DATABASE", "OTHER_DB", "ROLE", "TEST_ROLE")
    val currentGrants = createCurrentGrants(futureGrant)

    // When
    val actualRevokes = compiler.compileRevokeGrants(JList.of(playbookGrant), currentGrants)

    // Then
    assertEquals(actualRevokes.size(), 1)
    val revokedGrant = actualRevokes.get(0).getGrant
    assertEquals(revokedGrant.privilege(), "USAGE")
    assertEquals(revokedGrant.grantedOn(), "DATABASE")
    assertEquals(revokedGrant.name(), "OTHER_DB")
    assertEquals(revokedGrant.future().booleanValue(), true)
  }

  test(
    "compileRevokeGrants when database all grant matches playbook should not revoke"
  ) {
    // Given
    val playbookGrant =
      createPlaybookGrant("DATABASE", "TEST_DB", null, null, List("USAGE"), true, true, true)
    val allGrant      = createAllGrant("USAGE", "DATABASE", "TEST_DB", "ROLE", "TEST_ROLE")
    val currentGrants = createCurrentGrants(allGrant)

    // When
    val actualRevokes = compiler.compileRevokeGrants(JList.of(playbookGrant), currentGrants)

    // Then
    assert(actualRevokes.isEmpty)
  }

  test(
    "compileRevokeGrants when database all grant does not match playbook should revoke"
  ) {
    // Given
    val playbookGrant =
      createPlaybookGrant("DATABASE", "TEST_DB", null, null, List("USAGE"), true, true, true)
    val allGrant      = createAllGrant("USAGE", "DATABASE", "OTHER_DB", "ROLE", "TEST_ROLE")
    val currentGrants = createCurrentGrants(allGrant)

    // When
    val actualRevokes = compiler.compileRevokeGrants(JList.of(playbookGrant), currentGrants)

    // Then
    assertEquals(actualRevokes.size(), 1)
    val revokedGrant = actualRevokes.get(0).getGrant
    assertEquals(revokedGrant.privilege(), "USAGE")
    assertEquals(revokedGrant.grantedOn(), "DATABASE")
    assertEquals(revokedGrant.name(), "OTHER_DB")
    assertEquals(revokedGrant.all().booleanValue(), true)
  }

  test("compileRevokeGrants should return results sorted by key") {
    // Given: Two grants that need to be revoked (not in playbook)
    // Grant 1: Key "GRANT SELECT ON TABLE DB.SCHEMA.TABLE_B TO ROLE ROLE_1"
    // Grant 2: Key "GRANT SELECT ON TABLE DB.SCHEMA.TABLE_A TO ROLE ROLE_1"

    // We intentionally create them in an order that is NOT sorted by key (B before A)
    val grantB = createGrant("SELECT", "TABLE", "DB.SCHEMA.TABLE_B", "ROLE", "ROLE_1")
    val grantA = createGrant("SELECT", "TABLE", "DB.SCHEMA.TABLE_A", "ROLE", "ROLE_1")

    // Playbook is empty, so everything should be revoked
    val playbookGrants = JList.of[PlaybookPrivilegeGrant]()

    // We can't use createCurrentGrants because we want to control the iteration order if possible,
    // although HashMap doesn't guarantee it. The test relies on the sorting logic in the compiler,
    // not the map order.
    val currentGrants = new HashMap[String, SnowflakeGrantBuilder]()
    val builderB      = SnowflakeGrantBuilder.fromGrant(grantB)
    val builderA      = SnowflakeGrantBuilder.fromGrant(grantA)

    currentGrants.put(builderB.getKey, builderB)
    currentGrants.put(builderA.getKey, builderA)

    // When
    val result = compiler.compileRevokeGrants(playbookGrants, currentGrants)

    // Then
    // We expect the result to be sorted by key, so builderA (ending in TABLE_A) should come before builderB (ending in TABLE_B)
    assertEquals(result.size(), 2)
    assertEquals(result.get(0).getKey, builderA.getKey)
    assertEquals(result.get(1).getKey, builderB.getKey)
  }

  // Helper methods for creating test data
  private def createPlaybookGrant(
    objectType: String,
    databaseName: String,
    schemaName: String,
    objectName: String,
    privileges: List[String]
  ): PlaybookPrivilegeGrant =
    createPlaybookGrant(
      objectType,
      databaseName,
      schemaName,
      objectName,
      privileges,
      includeFuture = true,
      includeAll = true,
      enable = true
    )

  private def createPlaybookGrant(
    objectType: String,
    databaseName: String,
    schemaName: String,
    objectName: String,
    privileges: List[String],
    includeFuture: Boolean,
    includeAll: Boolean,
    enable: Boolean
  ): PlaybookPrivilegeGrant =
    new PlaybookPrivilegeGrant(
      objectType,
      objectName,
      schemaName,
      databaseName,
      privileges.asJava,
      includeFuture,
      includeAll,
      enable
    )

  private def createPlaybookGrantForRole(
    roleName: String,
    privileges: List[String]
  ): PlaybookPrivilegeGrant =
    new PlaybookPrivilegeGrant("ROLE", roleName, null, null, privileges.asJava, true, true, true)

  private def createGrant(
    privilege: String,
    grantedOn: String,
    name: String,
    grantedTo: String,
    granteeName: String
  ): SnowflakeGrantModel =
    new SnowflakeGrantModel(privilege, grantedOn, name, grantedTo, granteeName, false, false, false)

  private def createFutureGrant(
    privilege: String,
    grantedOn: String,
    name: String,
    grantedTo: String,
    granteeName: String
  ): SnowflakeGrantModel =
    new SnowflakeGrantModel(privilege, grantedOn, name, grantedTo, granteeName, false, true, false)

  private def createAllGrant(
    privilege: String,
    grantedOn: String,
    name: String,
    grantedTo: String,
    granteeName: String
  ): SnowflakeGrantModel =
    new SnowflakeGrantModel(privilege, grantedOn, name, grantedTo, granteeName, false, false, true)

  private def createCurrentGrants(
    grants: SnowflakeGrantModel*
  ): JMap[String, SnowflakeGrantBuilder] =
    val currentGrants = new HashMap[String, SnowflakeGrantBuilder]()
    for grant <- grants do
      try
        val builder = SnowflakeGrantBuilder.fromGrant(grant)
        if builder != null then currentGrants.put(builder.getKey, builder)
      catch
        case _: Exception =>
        // Skip grants that cannot be built
    currentGrants
