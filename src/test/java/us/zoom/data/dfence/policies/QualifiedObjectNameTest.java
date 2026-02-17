package us.zoom.data.dfence.policies;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.common.collect.ImmutableList;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import us.zoom.data.dfence.playbook.model.PlaybookPrivilegeGrant;
import us.zoom.data.dfence.policies.factories.PolicyGrantFactory;
import us.zoom.data.dfence.policies.models.PolicyGrant;
import us.zoom.data.dfence.policies.pattern.models.PolicyType;

class QualifiedObjectNameTest {

  @Test
  @DisplayName("Table grant: qualified name quotes database with special chars")
  void tableGrant_databaseWithSpecialChars_quotedInQualifiedObjectName() {
    PlaybookPrivilegeGrant grant =
        new PlaybookPrivilegeGrant(
            "table",
            "MY_TABLE",
            "MY_SCHEMA",
            """
            "foo123%.^bar"
            """.strip(),
            ImmutableList.of("SELECT"),
            false,
            false,
            true);

    PolicyGrant policyGrant =
        PolicyGrantFactory.createFrom(grant).getOrElseThrow(AssertionError::new);

    assertInstanceOf(PolicyType.Standard.SchemaObject.class, policyGrant.policyType());

    String normalizedObjectName = policyGrant.policyType().qualifiedObjectName();
    assertEquals(
        """
        "foo123%.^bar".MY_SCHEMA.MY_TABLE
        """.strip(), normalizedObjectName);
  }

  @Test
  @DisplayName("Schema grant: qualified name preserves quoted schema")
  void schemaGrant_quotedSchema_qualifiedObjectNamePreservesQuoting() {
    PlaybookPrivilegeGrant grant =
        new PlaybookPrivilegeGrant(
            "schema",
            null,
            """
            "My.Schema.Name"
            """.strip(),
            "MY_DB",
            ImmutableList.of("USAGE"),
            false,
            false,
            true);

    PolicyGrant policyGrant =
        PolicyGrantFactory.createFrom(grant).getOrElseThrow(AssertionError::new);

    assertInstanceOf(PolicyType.Standard.DatabaseObject.class, policyGrant.policyType());

    String normalizedObjectName = policyGrant.policyType().qualifiedObjectName();
    assertEquals("""
        MY_DB."My.Schema.Name"
        """.strip(), normalizedObjectName);
  }

  @Test
  @DisplayName("Database grant: qualified name is quoted database")
  void databaseGrant_quotedDatabase_qualifiedObjectName() {
    PlaybookPrivilegeGrant grant =
        new PlaybookPrivilegeGrant(
            "database",
            null,
            null,
            """
            "My.Database"
            """.strip(),
            ImmutableList.of("USAGE"),
            false,
            false,
            true);

    PolicyGrant policyGrant =
        PolicyGrantFactory.createFrom(grant).getOrElseThrow(AssertionError::new);

    assertTrue(policyGrant.policyType() instanceof PolicyType.Standard.AccountObject);

    String normalizedObjectName = policyGrant.policyType().qualifiedObjectName();
    assertEquals("""
        "My.Database"
        """.strip(), normalizedObjectName);
  }
}
