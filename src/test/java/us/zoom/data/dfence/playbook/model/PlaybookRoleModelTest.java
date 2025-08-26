package us.zoom.data.dfence.playbook.model;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;

import org.junit.jupiter.api.Test;

import us.zoom.data.dfence.providers.snowflake.grant.builder.options.UnsupportedRevokeBehavior;

class PlaybookRoleModelTest {

  @Test
  void testPlaybookRoleModelWithUnsupportedRevokeBehavior() {
    PlaybookRoleModel role =
        new PlaybookRoleModel(
            "test_role",
            List.of(),
            true,
            true,
            true,
            "SECURITYADMIN",
            UnsupportedRevokeBehavior.DROP);

    assertEquals("test_role", role.name());
    assertEquals(List.of(), role.grants());
    assertTrue(role.create());
    assertTrue(role.revokeOtherGrants());
    assertTrue(role.enable());
    assertEquals("SECURITYADMIN", role.roleOwner());
    assertEquals(UnsupportedRevokeBehavior.DROP, role.unsupportedRevokeBehavior());
  }

  @Test
  void testPlaybookRoleModelWithNullUnsupportedRevokeBehavior() {
    PlaybookRoleModel role =
        new PlaybookRoleModel("test_role", List.of(), true, true, true, "SECURITYADMIN", null);

    assertEquals("test_role", role.name());
    assertEquals(List.of(), role.grants());
    assertTrue(role.create());
    assertTrue(role.revokeOtherGrants());
    assertTrue(role.enable());
    assertEquals("SECURITYADMIN", role.roleOwner());
    assertEquals(
        UnsupportedRevokeBehavior.IGNORE, role.unsupportedRevokeBehavior()); // Default value
  }

  @Test
  void testPlaybookRoleModelWithNullValues() {
    PlaybookRoleModel role = new PlaybookRoleModel("test_role", null, null, null, null, null, null);

    assertEquals("test_role", role.name());
    assertEquals(List.of(), role.grants()); // Default to empty list
    assertTrue(role.create()); // Default to true
    assertTrue(role.revokeOtherGrants()); // Default to true
    assertTrue(role.enable()); // Default to true
    assertNull(role.roleOwner());
    assertEquals(
        UnsupportedRevokeBehavior.IGNORE, role.unsupportedRevokeBehavior()); // Default value
  }

  @Test
  void testPlaybookRoleModelWithMinimalConstructor() {
    PlaybookRoleModel role = new PlaybookRoleModel("test_role", List.of());

    assertEquals("test_role", role.name());
    assertEquals(List.of(), role.grants());
    assertTrue(role.create()); // Default to true
    assertTrue(role.revokeOtherGrants()); // Default to true
    assertTrue(role.enable()); // Default to true
    assertNull(role.roleOwner());
    assertEquals(
        UnsupportedRevokeBehavior.IGNORE, role.unsupportedRevokeBehavior()); // Default value
  }

  @Test
  void testPlaybookRoleModelWithGrants() {
    List<PlaybookPrivilegeGrant> grants =
        List.of(
            new PlaybookPrivilegeGrant(
                "table", "test_table", "test_schema", "test_db", List.of("select"), true, true));

    PlaybookRoleModel role =
        new PlaybookRoleModel(
            "test_role", grants, true, true, true, "SECURITYADMIN", UnsupportedRevokeBehavior.DROP);

    assertEquals("test_role", role.name());
    assertEquals(1, role.grants().size());
    assertEquals("test_table", role.grants().get(0).objectName());
    assertEquals("SECURITYADMIN", role.roleOwner());
    assertEquals(UnsupportedRevokeBehavior.DROP, role.unsupportedRevokeBehavior());
  }

  @Test
  void testPlaybookRoleModelImmutability() {
    List<PlaybookPrivilegeGrant> originalGrants =
        List.of(
            new PlaybookPrivilegeGrant(
                "table", "test_table", "test_schema", "test_db", List.of("select"), true, true));

    PlaybookRoleModel role =
        new PlaybookRoleModel(
            "test_role",
            originalGrants,
            true,
            true,
            true,
            "SECURITYADMIN",
            UnsupportedRevokeBehavior.DROP);

    // Verify that the grants list is immutable
    assertThrows(
        UnsupportedOperationException.class,
        () -> {
          role.grants()
              .add(
                  new PlaybookPrivilegeGrant(
                      "table",
                      "another_table",
                      "test_schema",
                      "test_db",
                      List.of("select"),
                      true,
                      true));
        });
  }

  @Test
  void testPlaybookRoleModelEquality() {
    PlaybookRoleModel role1 =
        new PlaybookRoleModel(
            "test_role",
            List.of(),
            true,
            true,
            true,
            "SECURITYADMIN",
            UnsupportedRevokeBehavior.DROP);

    PlaybookRoleModel role2 =
        new PlaybookRoleModel(
            "test_role",
            List.of(),
            true,
            true,
            true,
            "SECURITYADMIN",
            UnsupportedRevokeBehavior.DROP);

    PlaybookRoleModel role3 =
        new PlaybookRoleModel(
            "test_role",
            List.of(),
            true,
            true,
            true,
            "SECURITYADMIN",
            UnsupportedRevokeBehavior.IGNORE);

    assertEquals(role1, role2);
    assertNotEquals(role1, role3);
    assertNotEquals(role2, role3);
  }

  @Test
  void testPlaybookRoleModelHashCode() {
    PlaybookRoleModel role1 =
        new PlaybookRoleModel(
            "test_role",
            List.of(),
            true,
            true,
            true,
            "SECURITYADMIN",
            UnsupportedRevokeBehavior.DROP);

    PlaybookRoleModel role2 =
        new PlaybookRoleModel(
            "test_role",
            List.of(),
            true,
            true,
            true,
            "SECURITYADMIN",
            UnsupportedRevokeBehavior.DROP);

    PlaybookRoleModel role3 =
        new PlaybookRoleModel(
            "test_role",
            List.of(),
            true,
            true,
            true,
            "SECURITYADMIN",
            UnsupportedRevokeBehavior.IGNORE);

    assertEquals(role1.hashCode(), role2.hashCode());
    assertNotEquals(role1.hashCode(), role3.hashCode());
  }

  @Test
  void testPlaybookRoleModelToString() {
    PlaybookRoleModel role =
        new PlaybookRoleModel(
            "test_role",
            List.of(),
            true,
            true,
            true,
            "SECURITYADMIN",
            UnsupportedRevokeBehavior.DROP);

    String toString = role.toString();

    assertTrue(toString.contains("test_role"));
    assertTrue(toString.contains("SECURITYADMIN"));
    assertTrue(toString.contains("DROP"));
  }

  @Test
  void testPlaybookRoleModelWithDifferentBooleanValues() {
    PlaybookRoleModel role =
        new PlaybookRoleModel(
            "test_role",
            List.of(),
            false,
            false,
            false,
            "SECURITYADMIN",
            UnsupportedRevokeBehavior.IGNORE);

    assertEquals("test_role", role.name());
    assertEquals(List.of(), role.grants());
    assertFalse(role.create());
    assertFalse(role.revokeOtherGrants());
    assertFalse(role.enable());
    assertEquals("SECURITYADMIN", role.roleOwner());
    assertEquals(UnsupportedRevokeBehavior.IGNORE, role.unsupportedRevokeBehavior());
  }
}
