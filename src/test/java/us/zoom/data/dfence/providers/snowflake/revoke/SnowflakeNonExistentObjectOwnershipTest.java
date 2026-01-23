package us.zoom.data.dfence.providers.snowflake.revoke;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ForkJoinPool;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import us.zoom.data.dfence.playbook.Playbook;
import us.zoom.data.dfence.playbook.model.PlaybookModel;
import us.zoom.data.dfence.providers.snowflake.SnowflakeGrantsService;
import us.zoom.data.dfence.providers.snowflake.SnowflakeProvider;
import us.zoom.data.dfence.providers.snowflake.SnowflakeStatementsService;
import us.zoom.data.dfence.providers.snowflake.grant.builder.SnowflakeGrantBuilder;
import us.zoom.data.dfence.providers.snowflake.informationschema.SnowflakeObjectsService;
import us.zoom.data.dfence.providers.snowflake.models.PartitionedGrantStatements;
import us.zoom.data.dfence.providers.snowflake.models.SnowflakeGrantModel;

/**
 * Tests that revoke statements (GRANT OWNERSHIP ... TO ROLE SECURITYADMIN) are not generated when
 * the object doesn't exist in SnowflakeObjectsService, except when checked by
 * SnowflakeObjectExistsFilter.
 */
class SnowflakeNonExistentObjectOwnershipTest {

  @Mock private SnowflakeGrantsService snowflakeGrantsService;

  @Mock private SnowflakeStatementsService snowflakeStatementsService;

  @Mock private SnowflakeObjectsService snowflakeObjectsService;

  private SnowflakeProvider snowflakeProvider;

  @BeforeEach
  void setUp() {
    MockitoAnnotations.openMocks(this);
    snowflakeProvider =
        new SnowflakeProvider(
            snowflakeStatementsService,
            snowflakeGrantsService,
            snowflakeObjectsService,
            ForkJoinPool.commonPool());
  }

  /**
   * Verifies that revoke statement is not generated when object doesn't exist in
   * SnowflakeObjectsService. The object exists only when checked by SnowflakeObjectExistsFilter
   * (via mock).
   */
  @Test
  void shouldNotGenerateRevokeStatementForNonExistentObjectOwnership()
      throws IOException, URISyntaxException {
    String databaseName = "DELETE_ME_USER_PROVISIONER_DB";
    String schemaName = "DELETE_ME_USER_PROVISIONER_TEST_USER1";
    String tableName = "TABLE_X";
    String fullTableName = databaseName + "." + schemaName + "." + tableName;

    // Mock: objectExists returns false by default, true when called from
    // SnowflakeObjectExistsFilter
    doAnswer(
            invocation -> {
              StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
              for (StackTraceElement element : stackTrace) {
                if (element.getClassName().contains("SnowflakeObjectExistsFilter")) {
                  return true;
                }
              }
              return false;
            })
        .when(snowflakeObjectsService)
        .objectExists(anyString(), any());

    PlaybookModel playbookModel = getUserProvisionerPlaybookModel();
    String ownerRoleName = "OWNER_DELETE_ME_USER_PROVISIONER_TEST_USER1";

    SnowflakeGrantModel ownershipGrant =
        new SnowflakeGrantModel(
            "OWNERSHIP", "TABLE", fullTableName, "ROLE", ownerRoleName, false, false, false);

    SnowflakeGrantBuilder ownershipGrantBuilder = SnowflakeGrantBuilder.fromGrant(ownershipGrant);
    assertNotNull(ownershipGrantBuilder, "Ownership grant builder should be created");

    HashMap<String, SnowflakeGrantBuilder> currentGrants = new HashMap<>();
    currentGrants.put(ownershipGrantBuilder.getKey(), ownershipGrantBuilder);

    when(snowflakeGrantsService.getGrants(ownerRoleName, false)).thenReturn(currentGrants);

    var ownerRole = playbookModel.roles().get(ownerRoleName);
    var privilegeGrants = ownerRole.grants();

    PartitionedGrantStatements partitionedStatements =
        snowflakeProvider.compilePlaybookPrivilegeGrants(
            privilegeGrants,
            ownerRoleName,
            true,
            true,
            false,
            playbookModel,
            false,
            ownerRole.unsupportedRevokeBehavior());
    List<List<String>> statements = new ArrayList<>();
    statements.addAll(partitionedStatements.ownershipStatements());
    statements.addAll(partitionedStatements.nonOwnershipStatements());

    String expectedRevokeStatement =
        String.format(
            "GRANT OWNERSHIP ON TABLE \"%s\".\"%s\".\"%s\" TO ROLE SECURITYADMIN COPY CURRENT GRANTS;",
            databaseName, schemaName, tableName);
    List<String> allStatements = statements.stream().flatMap(List::stream).toList();

    assertFalse(
        allStatements.contains(expectedRevokeStatement),
        "Revoke statement should be filtered out by SnowflakeOwnedObjectFilter when another role has ownership");
  }

  private PlaybookModel getUserProvisionerPlaybookModel() throws IOException, URISyntaxException {
    var resourceUrl =
        getClass()
            .getClassLoader()
            .getResource("test-data/snowflake-non-existent-object-ownership-test.yml");
    if (resourceUrl == null) {
      throw new RuntimeException(
          "Could not find snowflake-non-existent-object-ownership-test.yml in test resources");
    }
    String yamlString = Files.readString(Path.of(resourceUrl.toURI()));
    return Playbook.parse(yamlString, new HashMap<>());
  }
}
