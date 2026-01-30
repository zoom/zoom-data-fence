package us.zoom.data.dfence.providers.snowflake.grant.create.internal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import us.zoom.data.dfence.providers.snowflake.grant.builder.SnowflakeObjectType;
import us.zoom.data.dfence.providers.snowflake.grant.desired.create.data.models.ContainerGrantsCreationData;
import us.zoom.data.dfence.providers.snowflake.grant.desired.create.internal.AllGrantsFactory;
import us.zoom.data.dfence.providers.snowflake.informationschema.SnowflakeObjectsService;
import us.zoom.data.dfence.providers.snowflake.models.SnowflakeGrantModel;
import us.zoom.data.dfence.policies.models.PolicyGrantPrivilege;

@DisplayName("AllGrantsFactory")
class AllGrantsFactoryTest {

  private SnowflakeObjectsService mockObjectsService;
  private AllGrantsFactory provider;

  @BeforeEach
  void setUp() {
    mockObjectsService = mock(SnowflakeObjectsService.class);
    provider = new AllGrantsFactory(mockObjectsService);
  }

  private static ContainerGrantsCreationData containerData(
      SnowflakeObjectType objectType,
      SnowflakeObjectType containerObjectType,
      String normalizedObjectName,
      List<PolicyGrantPrivilege> privileges,
      String roleName) {
    return new ContainerGrantsCreationData(
        objectType,
        containerObjectType,
        normalizedObjectName,
        false,
        privileges,
        roleName);
  }

  @Nested
  @DisplayName("createFrom")
  class CreateGrants {

    @Test
    @DisplayName("returns empty when container does not exist")
    void containerMissingReturnsEmpty() {
      when(mockObjectsService.objectExists(eq("MY_DB.MY_SCHEMA"), eq(SnowflakeObjectType.SCHEMA)))
          .thenReturn(false);
      ContainerGrantsCreationData data =
          containerData(
              SnowflakeObjectType.TABLE,
              SnowflakeObjectType.SCHEMA,
              "MY_DB.MY_SCHEMA",
              List.of(new PolicyGrantPrivilege("SELECT")),
              "MY_ROLE");

      List<SnowflakeGrantModel> result = provider.createFrom(data);

      assertEquals(0, result.size());
    }

    @Test
    @DisplayName("returns one grant per object per privilege when container exists")
    void containerExistsExpandsToAllObjectsAndPrivileges() {
      when(mockObjectsService.objectExists(eq("MY_DB.MY_SCHEMA"), eq(SnowflakeObjectType.SCHEMA)))
          .thenReturn(true);
      when(mockObjectsService.getContainerObjectQualNames(
              eq(SnowflakeObjectType.SCHEMA), eq(SnowflakeObjectType.TABLE), eq("MY_DB.MY_SCHEMA")))
          .thenReturn(
              List.of(
                  "MY_DB.MY_SCHEMA.TABLE1",
                  "MY_DB.MY_SCHEMA.TABLE2"));
      ContainerGrantsCreationData data =
          containerData(
              SnowflakeObjectType.TABLE,
              SnowflakeObjectType.SCHEMA,
              "MY_DB.MY_SCHEMA",
              List.of(new PolicyGrantPrivilege("SELECT"), new PolicyGrantPrivilege("INSERT")),
              "MY_ROLE");

      List<SnowflakeGrantModel> result = provider.createFrom(data);

      assertEquals(4, result.size());
      result.forEach(
          g -> {
            assertFalse(g.future());
            assertFalse(g.all());
            assertEquals("ROLE", g.grantedTo());
            assertEquals("MY_ROLE", g.granteeName());
          });
      long selectCount = result.stream().filter(g -> "SELECT".equals(g.privilege())).count();
      long insertCount = result.stream().filter(g -> "INSERT".equals(g.privilege())).count();
      assertEquals(2, selectCount);
      assertEquals(2, insertCount);
      long table1Count = result.stream().filter(g -> g.name().contains("TABLE1")).count();
      long table2Count = result.stream().filter(g -> g.name().contains("TABLE2")).count();
      assertEquals(2, table1Count);
      assertEquals(2, table2Count);
    }

    @Test
    @DisplayName("uses container object type and object type for grantedOn")
    void grantedOnReflectsObjectType() {
      when(mockObjectsService.objectExists(eq("MY_DB"), eq(SnowflakeObjectType.DATABASE)))
          .thenReturn(true);
      when(mockObjectsService.getContainerObjectQualNames(
              eq(SnowflakeObjectType.DATABASE), eq(SnowflakeObjectType.VIEW), eq("MY_DB")))
          .thenReturn(List.of("MY_DB.SCHEMA1.VIEW1"));
      ContainerGrantsCreationData data =
          containerData(
              SnowflakeObjectType.VIEW,
              SnowflakeObjectType.DATABASE,
              "MY_DB",
              List.of(new PolicyGrantPrivilege("SELECT")),
              "MY_ROLE");

      List<SnowflakeGrantModel> result = provider.createFrom(data);

      assertEquals(1, result.size());
      assertEquals("VIEW", result.get(0).grantedOn());
      assertTrue(result.get(0).name().contains("VIEW1"));
    }
  }
}
