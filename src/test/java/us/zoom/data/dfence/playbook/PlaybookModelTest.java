package us.zoom.data.dfence.playbook;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import us.zoom.data.dfence.exception.RbacDataError;
import us.zoom.data.dfence.playbook.model.PlaybookModel;
import us.zoom.data.dfence.playbook.model.PlaybookRoleModel;
import us.zoom.data.dfence.providers.snowflake.grant.builder.options.UnsupportedRevokeBehavior;

class PlaybookModelTest {

  @Test
  void testPlaybookModelWithUnsupportedRevokeBehavior() {
    PlaybookModel playbook =
        new PlaybookModel(
            Map.of("test-role", new PlaybookRoleModel("test_role", List.of())),
            "SECURITYADMIN",
            UnsupportedRevokeBehavior.DROP);

    assertEquals("SECURITYADMIN", playbook.roleOwner());
    assertEquals(UnsupportedRevokeBehavior.DROP, playbook.unsupportedRevokeBehavior());
    assertEquals(1, playbook.roles().size());
    assertTrue(playbook.roles().containsKey("test-role"));
  }

  @Test
  void testPlaybookModelWithNullUnsupportedRevokeBehavior() {
    PlaybookModel playbook =
        new PlaybookModel(
            Map.of("test-role", new PlaybookRoleModel("test_role", List.of())),
            "SECURITYADMIN",
            null);

    assertEquals("SECURITYADMIN", playbook.roleOwner());
    assertNull(playbook.unsupportedRevokeBehavior());
  }

  @Test
  void testPlaybookModelMergeWithSameUnsupportedRevokeBehavior() {
    PlaybookModel playbook1 =
        new PlaybookModel(
            Map.of("role-1", new PlaybookRoleModel("role_1", List.of())),
            "SECURITYADMIN",
            UnsupportedRevokeBehavior.IGNORE);

    PlaybookModel playbook2 =
        new PlaybookModel(
            Map.of("role-2", new PlaybookRoleModel("role_2", List.of())),
            "SECURITYADMIN",
            UnsupportedRevokeBehavior.IGNORE);

    PlaybookModel merged = PlaybookModel.merge(List.of(playbook1, playbook2));

    assertEquals("SECURITYADMIN", merged.roleOwner());
    assertEquals(UnsupportedRevokeBehavior.IGNORE, merged.unsupportedRevokeBehavior());
    assertEquals(2, merged.roles().size());
    assertTrue(merged.roles().containsKey("role-1"));
    assertTrue(merged.roles().containsKey("role-2"));
  }

  @Test
  void testPlaybookModelMergeWithDifferentUnsupportedRevokeBehavior() {
    PlaybookModel playbook1 =
        new PlaybookModel(
            Map.of("role-1", new PlaybookRoleModel("role_1", List.of())),
            "SECURITYADMIN",
            UnsupportedRevokeBehavior.IGNORE);

    PlaybookModel playbook2 =
        new PlaybookModel(
            Map.of("role-2", new PlaybookRoleModel("role_2", List.of())),
            "SECURITYADMIN",
            UnsupportedRevokeBehavior.DROP);

    RbacDataError error =
        assertThrows(
            RbacDataError.class,
            () -> {
              PlaybookModel.merge(List.of(playbook1, playbook2));
            });

    assertTrue(
        error
            .getMessage()
            .contains("Unsupported revoke behavior is declared more than once with values"));
  }

  @Test
  void testPlaybookModelMergeWithOneNullUnsupportedRevokeBehavior() {
    PlaybookModel playbook1 =
        new PlaybookModel(
            Map.of("role-1", new PlaybookRoleModel("role_1", List.of())),
            "SECURITYADMIN",
            UnsupportedRevokeBehavior.IGNORE);

    PlaybookModel playbook2 =
        new PlaybookModel(
            Map.of("role-2", new PlaybookRoleModel("role_2", List.of())), "SECURITYADMIN", null);

    PlaybookModel merged = PlaybookModel.merge(List.of(playbook1, playbook2));

    assertEquals("SECURITYADMIN", merged.roleOwner());
    assertEquals(UnsupportedRevokeBehavior.IGNORE, merged.unsupportedRevokeBehavior());
    assertEquals(2, merged.roles().size());
  }

  @Test
  void testPlaybookModelMergeWithBothNullUnsupportedRevokeBehavior() {
    PlaybookModel playbook1 =
        new PlaybookModel(
            Map.of("role-1", new PlaybookRoleModel("role_1", List.of())), "SECURITYADMIN", null);

    PlaybookModel playbook2 =
        new PlaybookModel(
            Map.of("role-2", new PlaybookRoleModel("role_2", List.of())), "SECURITYADMIN", null);

    PlaybookModel merged = PlaybookModel.merge(List.of(playbook1, playbook2));

    assertEquals("SECURITYADMIN", merged.roleOwner());
    assertNull(merged.unsupportedRevokeBehavior());
    assertEquals(2, merged.roles().size());
  }

  @Test
  void testPlaybookModelMergeWithDuplicateRoleKeys() {
    PlaybookModel playbook1 =
        new PlaybookModel(
            Map.of("duplicate-role", new PlaybookRoleModel("duplicate_role", List.of())),
            "SECURITYADMIN",
            UnsupportedRevokeBehavior.IGNORE);

    PlaybookModel playbook2 =
        new PlaybookModel(
            Map.of("duplicate-role", new PlaybookRoleModel("duplicate_role", List.of())),
            "SECURITYADMIN",
            UnsupportedRevokeBehavior.IGNORE);

    RbacDataError error =
        assertThrows(
            RbacDataError.class,
            () -> {
              PlaybookModel.merge(List.of(playbook1, playbook2));
            });

    assertTrue(
        error
            .getMessage()
            .contains("Cannot merge playbook models. The following role keys are duplicates"));
  }

  @Test
  void testPlaybookModelMergeWithDifferentRoleOwners() {
    PlaybookModel playbook1 =
        new PlaybookModel(
            Map.of("role-1", new PlaybookRoleModel("role_1", List.of())),
            "SECURITYADMIN",
            UnsupportedRevokeBehavior.IGNORE);

    PlaybookModel playbook2 =
        new PlaybookModel(
            Map.of("role-2", new PlaybookRoleModel("role_2", List.of())),
            "ACCOUNTADMIN",
            UnsupportedRevokeBehavior.IGNORE);

    RbacDataError error =
        assertThrows(
            RbacDataError.class,
            () -> {
              PlaybookModel.merge(List.of(playbook1, playbook2));
            });

    assertTrue(error.getMessage().contains("Role owner is declared more than once"));
  }

  @Test
  void testPlaybookModelMergeWithOneNullRoleOwner() {
    PlaybookModel playbook1 =
        new PlaybookModel(
            Map.of("role-1", new PlaybookRoleModel("role_1", List.of())),
            "SECURITYADMIN",
            UnsupportedRevokeBehavior.IGNORE);

    PlaybookModel playbook2 =
        new PlaybookModel(
            Map.of("role-2", new PlaybookRoleModel("role_2", List.of())),
            null,
            UnsupportedRevokeBehavior.IGNORE);

    PlaybookModel merged = PlaybookModel.merge(List.of(playbook1, playbook2));

    assertEquals("SECURITYADMIN", merged.roleOwner());
    assertEquals(UnsupportedRevokeBehavior.IGNORE, merged.unsupportedRevokeBehavior());
    assertEquals(2, merged.roles().size());
  }

  @Test
  void testPlaybookModelMergeWithBothNullRoleOwners() {
    PlaybookModel playbook1 =
        new PlaybookModel(
            Map.of("role-1", new PlaybookRoleModel("role_1", List.of())),
            null,
            UnsupportedRevokeBehavior.IGNORE);

    PlaybookModel playbook2 =
        new PlaybookModel(
            Map.of("role-2", new PlaybookRoleModel("role_2", List.of())),
            null,
            UnsupportedRevokeBehavior.IGNORE);

    PlaybookModel merged = PlaybookModel.merge(List.of(playbook1, playbook2));

    assertNull(merged.roleOwner());
    assertEquals(UnsupportedRevokeBehavior.IGNORE, merged.unsupportedRevokeBehavior());
    assertEquals(2, merged.roles().size());
  }

  @Test
  void testPlaybookModelImmutability() {
    Map<String, PlaybookRoleModel> originalRoles =
        Map.of("test-role", new PlaybookRoleModel("test_role", List.of()));
    PlaybookModel playbook =
        new PlaybookModel(originalRoles, "SECURITYADMIN", UnsupportedRevokeBehavior.DROP);

    // Verify that the roles map is immutable
    assertThrows(
        UnsupportedOperationException.class,
        () -> {
          playbook.roles().put("another-role", new PlaybookRoleModel("another_role", List.of()));
        });
  }
}
