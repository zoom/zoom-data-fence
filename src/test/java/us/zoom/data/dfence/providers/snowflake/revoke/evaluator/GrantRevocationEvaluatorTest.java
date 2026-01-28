package us.zoom.data.dfence.providers.snowflake.revoke.evaluator;

import static org.junit.jupiter.api.Assertions.*;

import com.google.common.collect.ImmutableList;
import io.vavr.control.Option;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import org.junit.jupiter.api.Test;
import us.zoom.data.dfence.exception.RbacDataError;
import us.zoom.data.dfence.providers.snowflake.grant.builder.SnowflakeObjectType;
import us.zoom.data.dfence.providers.snowflake.models.SnowflakeGrantModel;
import us.zoom.data.dfence.providers.snowflake.revoke.models.PlaybookGrant;
import us.zoom.data.dfence.providers.snowflake.revoke.models.PlaybookGrantHashIndex;
import us.zoom.data.dfence.providers.snowflake.revoke.models.PlaybookPattern;
import us.zoom.data.dfence.providers.snowflake.revoke.models.wrappers.GrantPrivilege;
import us.zoom.data.dfence.providers.snowflake.revoke.models.wrappers.ObjectType;
import us.zoom.data.dfence.providers.snowflake.revoke.models.wrappers.ObjectTypeAlias;
import us.zoom.data.dfence.providers.snowflake.validations.playbook.pattern.models.ResolvedPlaybookPattern;

class GrantRevocationEvaluatorTest {

  @Test
  void needsRevoke_shouldReturnTrue_whenNoMatchingPlaybookGrantExists() {
    PlaybookGrantHashIndex index = createEmptyIndex();
    GrantRevocationEvaluator evaluator = new GrantRevocationEvaluator(index);
    SnowflakeGrantModel grantToCheck = createGrantModel("SELECT", "TABLE", "DB.SCHEMA.TABLE");

    boolean actualNeedsRevoke = evaluator.needsRevoke(grantToCheck);

    assertTrue(actualNeedsRevoke, "Should revoke grant not found in playbook");
  }

  @Test
  void needsRevoke_shouldReturnFalse_whenMatchingPlaybookGrantExists() {
    PlaybookGrant playbookGrant =
        createPlaybookGrant("SELECT", SnowflakeObjectType.TABLE, "DB", "SCHEMA", "TABLE");
    PlaybookGrantHashIndex index = createIndexWith(playbookGrant);
    GrantRevocationEvaluator evaluator = new GrantRevocationEvaluator(index);
    SnowflakeGrantModel grantToCheck = createGrantModel("SELECT", "TABLE", "DB.SCHEMA.TABLE");

    boolean actualNeedsRevoke = evaluator.needsRevoke(grantToCheck);

    assertFalse(actualNeedsRevoke, "Should NOT revoke grant found in playbook");
  }

  @Test
  void needsRevoke_shouldReturnFalse_whenAliasMatches() {
    PlaybookGrant playbookGrant =
        createPlaybookGrant(
            "SELECT", SnowflakeObjectType.EXTERNAL_TABLE, "DB", "SCHEMA", "EXT_TABLE");
    PlaybookGrantHashIndex index = createIndexWith(playbookGrant);
    GrantRevocationEvaluator evaluator = new GrantRevocationEvaluator(index);
    SnowflakeGrantModel grantToCheck =
        createGrantModel("SELECT", "EXTERNAL_TABLE", "DB.SCHEMA.EXT_TABLE");

    boolean actualNeedsRevoke = evaluator.needsRevoke(grantToCheck);

    assertFalse(actualNeedsRevoke, "Should NOT revoke when object types are valid aliases");
  }

  @Test
  void needsRevoke_shouldReturnFalse_whenPlaybookHasAgentAndGrantIsCortexAgent() {
    // Critical: Playbook with AGENT (mapped to CORTEX_AGENT) should match CORTEX_AGENT grants
    PlaybookGrant playbookGrant =
        createPlaybookGrant(
            "USAGE",
            SnowflakeObjectType.CORTEX_AGENT, // AGENT string in playbook maps to CORTEX_AGENT enum
            "DB",
            "SCHEMA",
            "AGENT_NAME");
    PlaybookGrantHashIndex index = createIndexWith(playbookGrant);
    GrantRevocationEvaluator evaluator = new GrantRevocationEvaluator(index);
    SnowflakeGrantModel grantToCheck =
        createGrantModel("USAGE", "CORTEX_AGENT", "DB.SCHEMA.AGENT_NAME");

    boolean actualNeedsRevoke = evaluator.needsRevoke(grantToCheck);

    assertFalse(
        actualNeedsRevoke,
        "Should NOT revoke when playbook has CORTEX_AGENT (from AGENT string) and grant is CORTEX_AGENT");
  }

  @Test
  void needsRevoke_shouldReturnFalse_whenPlaybookHasCortexAgentAndGrantIsAgent() {
    // Critical: Grant with AGENT string (mapped to CORTEX_AGENT) should match CORTEX_AGENT playbook
    // grants
    PlaybookGrant playbookGrant =
        createPlaybookGrant(
            "USAGE", SnowflakeObjectType.CORTEX_AGENT, "DB", "SCHEMA", "AGENT_NAME");
    PlaybookGrantHashIndex index = createIndexWith(playbookGrant);
    GrantRevocationEvaluator evaluator = new GrantRevocationEvaluator(index);
    SnowflakeGrantModel grantToCheck =
        createGrantModel("USAGE", "AGENT", "DB.SCHEMA.AGENT_NAME"); // AGENT maps to CORTEX_AGENT

    boolean actualNeedsRevoke = evaluator.needsRevoke(grantToCheck);

    assertFalse(
        actualNeedsRevoke,
        "Should NOT revoke when playbook has CORTEX_AGENT and grant is AGENT (mapped to CORTEX_AGENT)");
  }

  @Test
  void needsRevoke_shouldReturnTrue_whenPlaybookHasAgentButPrivilegeDiffers() {
    // Critical: Even if object types match, privilege mismatch should cause revocation
    PlaybookGrant playbookGrant =
        createPlaybookGrant(
            "USAGE", SnowflakeObjectType.CORTEX_AGENT, "DB", "SCHEMA", "AGENT_NAME");
    PlaybookGrantHashIndex index = createIndexWith(playbookGrant);
    GrantRevocationEvaluator evaluator = new GrantRevocationEvaluator(index);
    SnowflakeGrantModel grantToCheck =
        createGrantModel("OWNERSHIP", "CORTEX_AGENT", "DB.SCHEMA.AGENT_NAME");

    boolean actualNeedsRevoke = evaluator.needsRevoke(grantToCheck);

    assertTrue(actualNeedsRevoke, "Should revoke when object types match but privilege differs");
  }

  @Test
  void needsRevoke_shouldThrowException_whenGrantModelHasInvalidObjectType() {
    // Critical: Invalid object types should throw exception
    PlaybookGrantHashIndex index = createEmptyIndex();
    GrantRevocationEvaluator evaluator = new GrantRevocationEvaluator(index);
    SnowflakeGrantModel grantToCheck =
        createGrantModel("SELECT", "INVALID_OBJECT_TYPE", "DB.SCHEMA.TABLE");

    assertThrows(
        RbacDataError.class,
        () -> evaluator.needsRevoke(grantToCheck),
        "Should throw exception when grant model has invalid object type");
  }

  @Test
  void needsRevoke_shouldReturnTrue_whenPrivilegeMatchesButObjectTypeDoesNot() {
    // Critical: Intersection logic - both privilege AND object type must match
    PlaybookGrant playbookGrant =
        createPlaybookGrant(
            "SELECT",
            SnowflakeObjectType.VIEW, // Different object type
            "DB",
            "SCHEMA",
            "TABLE");
    PlaybookGrantHashIndex index = createIndexWith(playbookGrant);
    GrantRevocationEvaluator evaluator = new GrantRevocationEvaluator(index);
    SnowflakeGrantModel grantToCheck = createGrantModel("SELECT", "TABLE", "DB.SCHEMA.TABLE");

    boolean actualNeedsRevoke = evaluator.needsRevoke(grantToCheck);

    assertTrue(
        actualNeedsRevoke,
        "Should revoke when privilege matches but object type doesn't (intersection is empty)");
  }

  @Test
  void needsRevoke_shouldReturnTrue_whenObjectTypeMatchesButPrivilegeDoesNot() {
    // Critical: Intersection logic - both privilege AND object type must match
    PlaybookGrant playbookGrant =
        createPlaybookGrant(
            "UPDATE", // Different privilege
            SnowflakeObjectType.TABLE,
            "DB",
            "SCHEMA",
            "TABLE");
    PlaybookGrantHashIndex index = createIndexWith(playbookGrant);
    GrantRevocationEvaluator evaluator = new GrantRevocationEvaluator(index);
    SnowflakeGrantModel grantToCheck = createGrantModel("SELECT", "TABLE", "DB.SCHEMA.TABLE");

    boolean actualNeedsRevoke = evaluator.needsRevoke(grantToCheck);

    assertTrue(
        actualNeedsRevoke,
        "Should revoke when object type matches but privilege doesn't (intersection is empty)");
  }

  @Test
  void needsRevoke_shouldReturnTrue_whenCandidatesSetIsEmpty() {
    // Critical: When intersection of object type and privilege grants is empty, should revoke
    PlaybookGrant privilegeGrant =
        createPlaybookGrant(
            "SELECT",
            SnowflakeObjectType.VIEW, // Different object type
            "DB",
            "SCHEMA",
            "TABLE");
    PlaybookGrant objectTypeGrant =
        createPlaybookGrant(
            "UPDATE", // Different privilege
            SnowflakeObjectType.TABLE,
            "DB",
            "SCHEMA",
            "TABLE");
    // Create index with grants that don't intersect
    PlaybookGrantHashIndex index =
        createIndexWithNonIntersectingGrants(privilegeGrant, objectTypeGrant);
    GrantRevocationEvaluator evaluator = new GrantRevocationEvaluator(index);
    SnowflakeGrantModel grantToCheck = createGrantModel("SELECT", "TABLE", "DB.SCHEMA.TABLE");

    boolean actualNeedsRevoke = evaluator.needsRevoke(grantToCheck);

    assertTrue(
        actualNeedsRevoke,
        "Should revoke when intersection of object type and privilege grants is empty");
  }

  @Test
  void needsRevoke_shouldReturnFalse_whenObjectTypeIndexAndAliasIndexBothContainMatchingGrant() {
    // Critical: Both object type index and alias index are checked, then unioned
    // This test verifies that grants in the alias index are properly included
    PlaybookGrant grant =
        createPlaybookGrant("SELECT", SnowflakeObjectType.TABLE, "DB", "SCHEMA", "TABLE");
    // Create index with grant in both indexes
    PlaybookGrantHashIndex index = createIndexWith(grant);
    GrantRevocationEvaluator evaluator = new GrantRevocationEvaluator(index);
    SnowflakeGrantModel grantToCheck = createGrantModel("SELECT", "TABLE", "DB.SCHEMA.TABLE");

    boolean actualNeedsRevoke = evaluator.needsRevoke(grantToCheck);

    assertFalse(
        actualNeedsRevoke,
        "Should not revoke when both object type and alias indexes contain matching grant");
  }

  @Test
  void needsRevoke_shouldThrowException_whenExceptionOccursDuringEvaluation() {
    // Critical: Exception handling - when conversion fails, should throw exception
    // Use an invalid object type that will cause SnowflakeObjectType.fromString to throw
    PlaybookGrantHashIndex index = createEmptyIndex();
    GrantRevocationEvaluator evaluator = new GrantRevocationEvaluator(index);
    // Create a grant model with invalid object type that will cause conversion to fail
    // We need to use a valid string format but invalid enum value
    SnowflakeGrantModel grantToCheck =
        new SnowflakeGrantModel(
            "SELECT",
            "INVALID_ENUM_VALUE_THAT_THROWS",
            "DB.SCHEMA.TABLE",
            "ROLE",
            "USER",
            false,
            false,
            false);

    assertThrows(
        RbacDataError.class,
        () -> evaluator.needsRevoke(grantToCheck),
        "Should throw exception when grant cannot be converted");
  }

  // Helper Functions
  private SnowflakeGrantModel createGrantModel(String priv, String objType, String name) {
    return new SnowflakeGrantModel(priv, objType, name, "ROLE", "USER", false, false, false);
  }

  private PlaybookGrant createPlaybookGrant(
      String priv, SnowflakeObjectType objType, String db, String schema, String obj) {
    return new PlaybookGrant(
        objType,
        new PlaybookPattern(Option.of(db), Option.of(schema), Option.of(obj)),
        ImmutableList.of(new GrantPrivilege(priv)),
        new ResolvedPlaybookPattern.Standard.SchemaObject(db, schema, obj),
        true);
  }

  private PlaybookGrantHashIndex createEmptyIndex() {
    return new PlaybookGrantHashIndex(
        new ConcurrentHashMap<>(), new ConcurrentHashMap<>(), new ConcurrentHashMap<>());
  }

  private PlaybookGrantHashIndex createIndexWith(PlaybookGrant grant) {
    GrantPrivilege priv = grant.privileges().get(0);
    ObjectType objType = ObjectType.apply(grant.objectType());
    ObjectTypeAlias alias = ObjectTypeAlias.apply(grant.objectType());

    ConcurrentHashMap<GrantPrivilege, Set<PlaybookGrant>> privilegeIndex =
        new ConcurrentHashMap<>();
    privilegeIndex.put(priv, Set.of(grant));

    ConcurrentHashMap<ObjectType, Set<PlaybookGrant>> objectTypeIndex = new ConcurrentHashMap<>();
    objectTypeIndex.put(objType, Set.of(grant));

    ConcurrentHashMap<ObjectTypeAlias, Set<PlaybookGrant>> objectAliasIndex =
        new ConcurrentHashMap<>();
    objectAliasIndex.put(alias, Set.of(grant));

    return new PlaybookGrantHashIndex(privilegeIndex, objectTypeIndex, objectAliasIndex);
  }

  private PlaybookGrantHashIndex createIndexWithNonIntersectingGrants(
      PlaybookGrant privilegeGrant, PlaybookGrant objectTypeGrant) {
    // Create index where grants don't intersect (different privileges and object types)
    GrantPrivilege priv1 = privilegeGrant.privileges().get(0);
    GrantPrivilege priv2 = objectTypeGrant.privileges().get(0);
    ObjectType objType1 = ObjectType.apply(privilegeGrant.objectType());
    ObjectType objType2 = ObjectType.apply(objectTypeGrant.objectType());
    ObjectTypeAlias alias1 = ObjectTypeAlias.apply(privilegeGrant.objectType());
    ObjectTypeAlias alias2 = ObjectTypeAlias.apply(objectTypeGrant.objectType());

    ConcurrentHashMap<GrantPrivilege, Set<PlaybookGrant>> privilegeIndex =
        new ConcurrentHashMap<>();
    privilegeIndex.put(priv1, Set.of(privilegeGrant));
    privilegeIndex.put(priv2, Set.of(objectTypeGrant));

    ConcurrentHashMap<ObjectType, Set<PlaybookGrant>> objectTypeIndex = new ConcurrentHashMap<>();
    objectTypeIndex.put(objType1, Set.of(privilegeGrant));
    objectTypeIndex.put(objType2, Set.of(objectTypeGrant));

    ConcurrentHashMap<ObjectTypeAlias, Set<PlaybookGrant>> objectAliasIndex =
        new ConcurrentHashMap<>();
    objectAliasIndex.put(alias1, Set.of(privilegeGrant));
    objectAliasIndex.put(alias2, Set.of(objectTypeGrant));

    return new PlaybookGrantHashIndex(privilegeIndex, objectTypeIndex, objectAliasIndex);
  }
}
