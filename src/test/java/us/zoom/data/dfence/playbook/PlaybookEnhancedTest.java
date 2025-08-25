package us.zoom.data.dfence.playbook;

import org.junit.jupiter.api.Test;
import us.zoom.data.dfence.playbook.model.PlaybookModel;
import us.zoom.data.dfence.playbook.model.PlaybookPrivilegeGrant;
import us.zoom.data.dfence.playbook.model.PlaybookRoleModel;
import us.zoom.data.dfence.providers.snowflake.grant.builder.options.UnsupportedRevokeBehavior;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class PlaybookEnhancedTest {

    @Test
    void testPropagateDefaultsWithUnsupportedRevokeBehavior() {
        PlaybookModel playbookModel = new PlaybookModel(
                Map.of(
                        "role-with-behavior",
                        new PlaybookRoleModel("role_with_behavior", List.of(), true, true, true, null, UnsupportedRevokeBehavior.DROP),
                        "role-without-behavior",
                        new PlaybookRoleModel("role_without_behavior", List.of(), true, true, true, null, null)
                ),
                "SECURITYADMIN",
                UnsupportedRevokeBehavior.IGNORE
        );

        PlaybookModel result = Playbook.propagateDefaults(playbookModel);

        // Role with behavior should keep its own behavior
        assertEquals(UnsupportedRevokeBehavior.DROP, 
                result.roles().get("role-with-behavior").unsupportedRevokeBehavior());
        
        // Role without behavior should inherit from playbook
        assertEquals(UnsupportedRevokeBehavior.IGNORE, 
                result.roles().get("role-without-behavior").unsupportedRevokeBehavior());
        
        // Playbook level should remain unchanged
        assertEquals(UnsupportedRevokeBehavior.IGNORE, result.unsupportedRevokeBehavior());
    }

    @Test
    void testPropagateDefaultsWithNullPlaybookUnsupportedRevokeBehavior() {
        PlaybookModel playbookModel = new PlaybookModel(
                Map.of(
                        "role-with-behavior",
                        new PlaybookRoleModel("role_with_behavior", List.of(), true, true, true, null, UnsupportedRevokeBehavior.DROP),
                        "role-without-behavior",
                        new PlaybookRoleModel("role_without_behavior", List.of(), true, true, true, null, null)
                ),
                "SECURITYADMIN",
                null
        );

        PlaybookModel result = Playbook.propagateDefaults(playbookModel);

        // Role with behavior should keep its own behavior
        assertEquals(UnsupportedRevokeBehavior.DROP, 
                result.roles().get("role-with-behavior").unsupportedRevokeBehavior());
        
        // Role without behavior should inherit from playbook (default behavior)
        assertEquals(UnsupportedRevokeBehavior.IGNORE, result.roles().get("role-without-behavior").unsupportedRevokeBehavior());
        
        // Playbook level should remain null
        assertNull(result.unsupportedRevokeBehavior());
    }

    @Test
    void testPropagateDefaultsWithRoleOwner() {
        PlaybookModel playbookModel = new PlaybookModel(
                Map.of(
                        "role-with-owner",
                        new PlaybookRoleModel("role_with_owner", List.of(), true, true, true, "ACCOUNTADMIN", UnsupportedRevokeBehavior.IGNORE),
                        "role-without-owner",
                        new PlaybookRoleModel("role_without_owner", List.of(), true, true, true, null, UnsupportedRevokeBehavior.IGNORE)
                ),
                "SECURITYADMIN",
                UnsupportedRevokeBehavior.IGNORE
        );

        PlaybookModel result = Playbook.propagateDefaults(playbookModel);

        // Role with owner should keep its own owner
        assertEquals("ACCOUNTADMIN", result.roles().get("role-with-owner").roleOwner());
        
        // Role without owner should inherit from playbook
        assertEquals("SECURITYADMIN", result.roles().get("role-without-owner").roleOwner());
        
        // Playbook level should remain unchanged
        assertEquals("SECURITYADMIN", result.roleOwner());
    }

    @Test
    void testPropagateDefaultsWithNullPlaybookRoleOwner() {
        PlaybookModel playbookModel = new PlaybookModel(
                Map.of(
                        "role-with-owner",
                        new PlaybookRoleModel("role_with_owner", List.of(), true, true, true, "ACCOUNTADMIN", UnsupportedRevokeBehavior.IGNORE),
                        "role-without-owner",
                        new PlaybookRoleModel("role_without_owner", List.of(), true, true, true, null, UnsupportedRevokeBehavior.IGNORE)
                ),
                null,
                UnsupportedRevokeBehavior.IGNORE
        );

        PlaybookModel result = Playbook.propagateDefaults(playbookModel);

        // Role with owner should keep its own owner
        assertEquals("ACCOUNTADMIN", result.roles().get("role-with-owner").roleOwner());
        
        // Role without owner should remain null
        assertNull(result.roles().get("role-without-owner").roleOwner());
        
        // Playbook level should remain null
        assertNull(result.roleOwner());
    }

    @Test
    void testPropagateDefaultsWithBothRoleOwnerAndUnsupportedRevokeBehavior() {
        PlaybookModel playbookModel = new PlaybookModel(
                Map.of(
                        "role-with-both",
                        new PlaybookRoleModel("role_with_both", List.of(), true, true, true, "ACCOUNTADMIN", UnsupportedRevokeBehavior.DROP),
                        "role-without-both",
                        new PlaybookRoleModel("role_without_both", List.of(), true, true, true, null, null)
                ),
                "SECURITYADMIN",
                UnsupportedRevokeBehavior.IGNORE
        );

        PlaybookModel result = Playbook.propagateDefaults(playbookModel);

        // Role with both should keep its own values
        assertEquals("ACCOUNTADMIN", result.roles().get("role-with-both").roleOwner());
        assertEquals(UnsupportedRevokeBehavior.DROP, result.roles().get("role-with-both").unsupportedRevokeBehavior());
        
        // Role without both should inherit from playbook
        assertEquals("SECURITYADMIN", result.roles().get("role-without-both").roleOwner());
        assertEquals(UnsupportedRevokeBehavior.IGNORE, result.roles().get("role-without-both").unsupportedRevokeBehavior());
    }

    @Test
    void testFilterPlaybookPreservesUnsupportedRevokeBehavior() {
        PlaybookModel playbookModel = new PlaybookModel(
                Map.of(
                        "enabled-role",
                        new PlaybookRoleModel("enabled_role", List.of(), true, true, true, "SECURITYADMIN", UnsupportedRevokeBehavior.DROP),
                        "disabled-role",
                        new PlaybookRoleModel("disabled_role", List.of(), true, true, false, "ACCOUNTADMIN", UnsupportedRevokeBehavior.IGNORE)
                ),
                "SECURITYADMIN",
                UnsupportedRevokeBehavior.IGNORE
        );

        PlaybookModel result = Playbook.filterPlaybook(playbookModel);

        // Should only contain enabled role
        assertEquals(1, result.roles().size());
        assertTrue(result.roles().containsKey("enabled-role"));
        assertFalse(result.roles().containsKey("disabled-role"));
        
        // Should preserve playbook level values
        assertEquals("SECURITYADMIN", result.roleOwner());
        assertEquals(UnsupportedRevokeBehavior.IGNORE, result.unsupportedRevokeBehavior());
    }

    @Test
    void testFilterRolePreservesUnsupportedRevokeBehavior() {
        PlaybookRoleModel roleModel = new PlaybookRoleModel(
                "test_role",
                List.of(
                        new PlaybookPrivilegeGrant("table", "test_table", "test_schema", "test_db", List.of("select"), true, true, true),
                        new PlaybookPrivilegeGrant("table", "test_table2", "test_schema", "test_db", List.of("select"), true, true, false)
                ),
                true,
                true,
                true,
                "SECURITYADMIN",
                UnsupportedRevokeBehavior.DROP
        );

        PlaybookRoleModel result = Playbook.filterRole(roleModel);

        // Should only contain enabled grants
        assertEquals(1, result.grants().size());
        assertEquals("test_table", result.grants().get(0).objectName());
        
        // Should preserve role level values
        assertEquals("SECURITYADMIN", result.roleOwner());
        assertEquals(UnsupportedRevokeBehavior.DROP, result.unsupportedRevokeBehavior());
    }

    @Test
    void testSerializeAndParseWithUnsupportedRevokeBehavior() throws Exception {
        PlaybookModel original = new PlaybookModel(
                Map.of("test-role", new PlaybookRoleModel("test_role", List.of())),
                "SECURITYADMIN",
                UnsupportedRevokeBehavior.DROP
        );

        String serialized = Playbook.serialize(original);
        PlaybookModel parsed = Playbook.parse(serialized, Map.of());

        assertEquals(original.roleOwner(), parsed.roleOwner());
        assertEquals(original.unsupportedRevokeBehavior(), parsed.unsupportedRevokeBehavior());
        assertEquals(original.roles().size(), parsed.roles().size());
    }

    @Test
    void testSerializeAndParseWithNullUnsupportedRevokeBehavior() throws Exception {
        PlaybookModel original = new PlaybookModel(
                Map.of("test-role", new PlaybookRoleModel("test_role", List.of())),
                "SECURITYADMIN",
                null
        );

        String serialized = Playbook.serialize(original);
        PlaybookModel parsed = Playbook.parse(serialized, Map.of());

        assertEquals(original.roleOwner(), parsed.roleOwner());
        assertEquals(original.unsupportedRevokeBehavior(), parsed.unsupportedRevokeBehavior());
        assertEquals(original.roles().size(), parsed.roles().size());
    }
}
