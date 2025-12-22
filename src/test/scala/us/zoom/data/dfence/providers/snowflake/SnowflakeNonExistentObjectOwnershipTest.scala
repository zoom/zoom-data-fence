package us.zoom.data.dfence.providers.snowflake

import munit.FunSuite
import org.mockito.ArgumentMatchers.{any, anyString}
import org.mockito.Mockito.{doAnswer, mock, reset, when}
import us.zoom.data.dfence.playbook.Playbook
import us.zoom.data.dfence.playbook.model.PlaybookModel
import us.zoom.data.dfence.providers.snowflake.grant.builder.{SnowflakeGrantBuilder, SnowflakeObjectType}
import us.zoom.data.dfence.providers.snowflake.informationschema.SnowflakeObjectsService
import us.zoom.data.dfence.providers.snowflake.models.SnowflakeGrantModel

import java.nio.file.{Files, Path}
import java.util.HashMap
import java.util.concurrent.ForkJoinPool
import scala.compiletime.uninitialized
import scala.jdk.CollectionConverters.*

/**
 * Tests that revoke statements (GRANT OWNERSHIP ... TO ROLE SECURITYADMIN) are not generated
 * when the object doesn't exist in SnowflakeObjectsService, except when checked by SnowflakeObjectExistsFilter.
 */
class SnowflakeNonExistentObjectOwnershipTest extends FunSuite:

  private val snowflakeGrantsService     = mock(classOf[SnowflakeGrantsService])
  private val snowflakeStatementsService = mock(classOf[SnowflakeStatementsService])
  private val snowflakeObjectsService    = mock(classOf[SnowflakeObjectsService])

  private var snowflakeProvider: SnowflakeProvider = uninitialized

  override def beforeEach(context: BeforeEach): Unit =
    reset(snowflakeGrantsService, snowflakeStatementsService, snowflakeObjectsService)
    snowflakeProvider = new SnowflakeProvider(
      snowflakeStatementsService,
      snowflakeGrantsService,
      snowflakeObjectsService,
      ForkJoinPool.commonPool()
    )

  /**
   * Verifies that revoke statement is not generated when object doesn't exist in SnowflakeObjectsService.
   * The object exists only when checked by SnowflakeObjectExistsFilter (via mock).
   */
  test("should not generate revoke statement for non-existent object ownership") {
    val databaseName  = "DELETE_ME_USER_PROVISIONER_DB"
    val schemaName    = "DELETE_ME_USER_PROVISIONER_TEST_USER1"
    val tableName     = "TABLE_X"
    val fullTableName = s"$databaseName.$schemaName.$tableName"

    // Mock: objectExists returns false by default, true when called from SnowflakeObjectExistsFilter
    doAnswer { invocation =>
      val stackTrace = Thread.currentThread().getStackTrace
      stackTrace.exists(_.getClassName.contains("SnowflakeObjectExistsFilter"))
    }.when(snowflakeObjectsService).objectExists(anyString(), any(classOf[SnowflakeObjectType]))

    val playbookModel = getUserProvisionerPlaybookModel()
    val ownerRoleName = "OWNER_DELETE_ME_USER_PROVISIONER_TEST_USER1"

    val ownershipGrant = new SnowflakeGrantModel(
      "OWNERSHIP",
      "TABLE",
      fullTableName,
      "ROLE",
      ownerRoleName,
      false,
      false,
      false
    )

    val ownershipGrantBuilder = SnowflakeGrantBuilder.fromGrant(ownershipGrant)
    assert(ownershipGrantBuilder != null, "Ownership grant builder should be created")

    val currentGrants = new HashMap[String, SnowflakeGrantBuilder]()
    currentGrants.put(ownershipGrantBuilder.getKey, ownershipGrantBuilder)

    when(snowflakeGrantsService.getGrants(ownerRoleName, false)).thenReturn(currentGrants)

    val ownerRole       = playbookModel.roles().get(ownerRoleName)
    val privilegeGrants = ownerRole.grants()

    val statements = snowflakeProvider.compilePlaybookPrivilegeGrants(
      privilegeGrants,
      ownerRoleName,
      true,
      true,
      false,
      playbookModel,
      false,
      ownerRole.unsupportedRevokeBehavior()
    )

    val expectedRevokeStatement =
      s"""GRANT OWNERSHIP ON TABLE "$databaseName"."$schemaName"."$tableName" TO ROLE SECURITYADMIN COPY CURRENT GRANTS;"""
    val allStatements           = statements.asScala.flatMap(_.asScala).toList

    assert(
      !allStatements.contains(expectedRevokeStatement),
      "Revoke statement should be filtered out by SnowflakeOwnedObjectFilter when another role has ownership"
    )
  }

  private def getUserProvisionerPlaybookModel(): PlaybookModel =
    val resourceUrl =
      getClass.getClassLoader.getResource("test-data/snowflake-non-existent-object-ownership-test.yml")
    if resourceUrl == null then
      throw new RuntimeException(
        "Could not find snowflake-non-existent-object-ownership-test.yml in test resources"
      )
    val yamlString  = Files.readString(Path.of(resourceUrl.toURI))
    Playbook.parse(yamlString, new HashMap())
