package us.zoom.data.dfence.providers.snowflake.revoke.evaluator;

import static org.junit.jupiter.api.Assertions.*;

import com.google.common.collect.ImmutableList;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.junit.jupiter.api.Test;
import us.zoom.data.dfence.exception.RbacDataError;
import us.zoom.data.dfence.policies.models.PolicyGrant;
import us.zoom.data.dfence.policies.models.PolicyGrantPrivilege;
import us.zoom.data.dfence.policies.pattern.models.PolicyType;
import us.zoom.data.dfence.providers.snowflake.grant.builder.SnowflakeObjectType;
import us.zoom.data.dfence.providers.snowflake.models.SnowflakeGrantModel;
import us.zoom.data.dfence.providers.snowflake.revoke.models.PolicyGrantHashIndex;

class GrantRevocationEvaluatorTest {

  @Test
  void needsRevoke_shouldReturnTrue_whenNoMatchingPlaybookGrantExists() {
    PolicyGrantHashIndex index = createEmptyIndex();
    GrantRevocationEvaluator evaluator = new GrantRevocationEvaluator(index);
    SnowflakeGrantModel grantToCheck = createGrantModel("SELECT", "TABLE", "DB.SCHEMA.TABLE");

    boolean actualNeedsRevoke = evaluator.needsRevoke(grantToCheck);

    assertTrue(actualNeedsRevoke, "Should revoke grant not found in playbook");
  }

  @Test
  void needsRevoke_shouldReturnFalse_whenMatchingPlaybookGrantExists() {
    PolicyGrant playbookGrant =
        createPolicyGrant("SELECT", SnowflakeObjectType.TABLE, "DB", "SCHEMA", "TABLE");
    PolicyGrantHashIndex index = createIndexWith(playbookGrant);
    GrantRevocationEvaluator evaluator = new GrantRevocationEvaluator(index);
    SnowflakeGrantModel grantToCheck = createGrantModel("SELECT", "TABLE", "DB.SCHEMA.TABLE");

    boolean actualNeedsRevoke = evaluator.needsRevoke(grantToCheck);

    assertFalse(actualNeedsRevoke, "Should NOT revoke grant found in playbook");
  }

  @Test
  void needsRevoke_shouldReturnFalse_whenAliasMatches() {
    PolicyGrant playbookGrant =
        createPolicyGrant(
            "SELECT", SnowflakeObjectType.EXTERNAL_TABLE, "DB", "SCHEMA", "EXT_TABLE");
    PolicyGrantHashIndex index = createIndexWith(playbookGrant);
    GrantRevocationEvaluator evaluator = new GrantRevocationEvaluator(index);
    SnowflakeGrantModel grantToCheck =
        createGrantModel("SELECT", "EXTERNAL_TABLE", "DB.SCHEMA.EXT_TABLE");

    boolean actualNeedsRevoke = evaluator.needsRevoke(grantToCheck);

    assertFalse(actualNeedsRevoke, "Should NOT revoke when object types are valid aliases");
  }

  @Test
  void needsRevoke_shouldReturnFalse_whenPlaybookHasAgentAndGrantIsCortexAgent() {
    // Critical: Playbook with AGENT (mapped to CORTEX_AGENT) should match CORTEX_AGENT grants
    PolicyGrant playbookGrant =
        createPolicyGrant(
            "USAGE",
            SnowflakeObjectType.CORTEX_AGENT, // AGENT string in playbook maps to CORTEX_AGENT enum
            "DB",
            "SCHEMA",
            "AGENT_NAME");
    PolicyGrantHashIndex index = createIndexWith(playbookGrant);
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
    PolicyGrant playbookGrant =
        createPolicyGrant("USAGE", SnowflakeObjectType.CORTEX_AGENT, "DB", "SCHEMA", "AGENT_NAME");
    PolicyGrantHashIndex index = createIndexWith(playbookGrant);
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
    PolicyGrant playbookGrant =
        createPolicyGrant("USAGE", SnowflakeObjectType.CORTEX_AGENT, "DB", "SCHEMA", "AGENT_NAME");
    PolicyGrantHashIndex index = createIndexWith(playbookGrant);
    GrantRevocationEvaluator evaluator = new GrantRevocationEvaluator(index);
    SnowflakeGrantModel grantToCheck =
        createGrantModel("OWNERSHIP", "CORTEX_AGENT", "DB.SCHEMA.AGENT_NAME");

    boolean actualNeedsRevoke = evaluator.needsRevoke(grantToCheck);

    assertTrue(actualNeedsRevoke, "Should revoke when object types match but privilege differs");
  }

  @Test
  void needsRevoke_shouldThrowException_whenGrantModelHasInvalidObjectType() {
    // Critical: Invalid object types should throw exception
    PolicyGrantHashIndex index = createEmptyIndex();
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
    PolicyGrant playbookGrant =
        createPolicyGrant(
            "SELECT",
            SnowflakeObjectType.VIEW, // Different object type
            "DB",
            "SCHEMA",
            "TABLE");
    PolicyGrantHashIndex index = createIndexWith(playbookGrant);
    GrantRevocationEvaluator evaluator = new GrantRevocationEvaluator(index);
    SnowflakeGrantModel grantToCheck = createGrantModel("SELECT", "TABLE", "DB.SCHEMA.TABLE");

    boolean actualNeedsRevoke = evaluator.needsRevoke(grantToCheck);

    assertTrue(
        actualNeedsRevoke,
        "Should revoke when privilege matches but object type doesn't (intersection is empty)");
  }

  @Test
  void needsRevoke_shouldReturnTrue_whenObjectTypeMatchesButPrivilegeDoesNot() {
    PolicyGrant playbookGrant =
        createPolicyGrant(
            "UPDATE", // Different privilege
            SnowflakeObjectType.TABLE,
            "DB",
            "SCHEMA",
            "TABLE");
    PolicyGrantHashIndex index = createIndexWith(playbookGrant);
    GrantRevocationEvaluator evaluator = new GrantRevocationEvaluator(index);
    SnowflakeGrantModel grantToCheck = createGrantModel("SELECT", "TABLE", "DB.SCHEMA.TABLE");

    boolean actualNeedsRevoke = evaluator.needsRevoke(grantToCheck);

    assertTrue(
        actualNeedsRevoke,
        "Should revoke when object type matches but privilege doesn't (intersection is empty)");
  }

  @Test
  void needsRevoke_shouldReturnFalse_whenIndexContainsMatchingGrant() {
    PolicyGrant grant =
        createPolicyGrant("SELECT", SnowflakeObjectType.TABLE, "DB", "SCHEMA", "TABLE");
    PolicyGrantHashIndex index = createIndexWith(grant);
    GrantRevocationEvaluator evaluator = new GrantRevocationEvaluator(index);
    SnowflakeGrantModel grantToCheck = createGrantModel("SELECT", "TABLE", "DB.SCHEMA.TABLE");

    boolean actualNeedsRevoke = evaluator.needsRevoke(grantToCheck);

    assertFalse(actualNeedsRevoke, "Should not revoke when index contains matching grant");
  }

  @Test
  void needsRevoke_shouldThrowException_whenExceptionOccursDuringEvaluation() {
    // Critical: Exception handling - when conversion fails, should throw exception
    // Use an invalid object type that will cause SnowflakeObjectType.fromString to throw
    PolicyGrantHashIndex index = createEmptyIndex();
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

  private PolicyGrant createPolicyGrant(
      String priv, SnowflakeObjectType objType, String db, String schema, String obj) {
    return new PolicyGrant(
        objType,
        ImmutableList.of(new PolicyGrantPrivilege(priv)),
        new PolicyType.Standard.SchemaObject(db, schema, obj),
        true);
  }

  private PolicyGrantHashIndex createEmptyIndex() {
    return new PolicyGrantHashIndex(new ConcurrentHashMap<>());
  }

  private PolicyGrantHashIndex createIndexWith(PolicyGrant grant) {
    ConcurrentHashMap<String, ConcurrentHashMap<PolicyGrantPrivilege, Set<PolicyGrant>>> index =
        new ConcurrentHashMap<>();

    String alias = grant.objectType().getAliasFor();
    index.computeIfAbsent(alias, k -> new ConcurrentHashMap<>());
    for (PolicyGrantPrivilege privilege : grant.privileges()) {
      index.get(alias).computeIfAbsent(privilege, k -> new HashSet<>()).add(grant);
    }

    return new PolicyGrantHashIndex(index);
  }
}
