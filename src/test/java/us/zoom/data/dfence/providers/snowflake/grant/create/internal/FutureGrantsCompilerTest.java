package us.zoom.data.dfence.providers.snowflake.grant.create.internal;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
import us.zoom.data.dfence.providers.snowflake.grant.desired.create.internal.FutureGrantsCompiler;
import us.zoom.data.dfence.providers.snowflake.informationschema.SnowflakeObjectsService;
import us.zoom.data.dfence.providers.snowflake.models.SnowflakeGrantModel;
import us.zoom.data.dfence.policies.models.PolicyGrantPrivilege;

@DisplayName("FutureGrantsFactory")
class FutureGrantsCompilerTest {

  private SnowflakeObjectsService mockObjectsService;
  private FutureGrantsCompiler provider;

  @BeforeEach
  void setUp() {
    mockObjectsService = mock(SnowflakeObjectsService.class);
    provider = new FutureGrantsCompiler(mockObjectsService);
  }

  private static ContainerGrantsCreationData containerData(
      SnowflakeObjectType objectType,
      String normalizedObjectName,
      List<PolicyGrantPrivilege> privileges,
      String roleName,
      boolean isSchemaObjectWithAllSchemas) {
    return new ContainerGrantsCreationData(
        objectType,
        SnowflakeObjectType.DATABASE,
        normalizedObjectName,
        isSchemaObjectWithAllSchemas,
        privileges,
        roleName);
  }

  @Nested
  @DisplayName("createFrom")
  class CreateGrants {

    @Test
    @DisplayName("when not schema-object-all-schemas returns one future grant per privilege on container")
    void singleContainerFutureGrants() {
      ContainerGrantsCreationData data =
          containerData(
              SnowflakeObjectType.VIEW,
              "MY_DB",
              List.of(new PolicyGrantPrivilege("SELECT"), new PolicyGrantPrivilege("REFERENCES")),
              "MY_ROLE",
              false);

      List<SnowflakeGrantModel> result = provider.createFrom(data);

      assertEquals(2, result.size());
      result.forEach(
          g -> {
            assertTrue(g.future());
            assertEquals(false, g.all());
            assertTrue(g.name().contains("MY_DB.<VIEW>"));
            assertEquals("ROLE", g.grantedTo());
            assertEquals("MY_ROLE", g.granteeName());
          });
      assertEquals("SELECT", result.get(0).privilege());
      assertEquals("REFERENCES", result.get(1).privilege());
    }

    @Test
    @DisplayName("when schema-object-all-schemas and database missing returns only database-level future grants")
    void schemaObjectAllSchemasDatabaseMissingSkipsSchemaGrants() {
      when(mockObjectsService.objectExists(eq("MY_DB"), eq(SnowflakeObjectType.DATABASE)))
          .thenReturn(false);
      ContainerGrantsCreationData data =
          containerData(
              SnowflakeObjectType.TABLE,
              "MY_DB",
              List.of(new PolicyGrantPrivilege("SELECT")),
              "MY_ROLE",
              true);

      List<SnowflakeGrantModel> result = provider.createFrom(data);

      assertEquals(1, result.size());
      assertTrue(result.get(0).name().contains("MY_DB.<TABLE>"));
    }

    @Test
    @DisplayName("when schema-object-all-schemas and database exists returns database-level and per-schema future grants")
    void schemaObjectAllSchemasDatabaseExistsAddsPerSchemaGrants() {
      when(mockObjectsService.objectExists(eq("MY_DB"), eq(SnowflakeObjectType.DATABASE)))
          .thenReturn(true);
      when(mockObjectsService.getContainerObjectQualNames(
              eq(SnowflakeObjectType.DATABASE), eq(SnowflakeObjectType.SCHEMA), eq("MY_DB")))
          .thenReturn(List.of("MY_DB.SCHEMA_A", "MY_DB.SCHEMA_B"));
      ContainerGrantsCreationData data =
          containerData(
              SnowflakeObjectType.VIEW,
              "MY_DB",
              List.of(new PolicyGrantPrivilege("SELECT")),
              "MY_ROLE",
              true);

      List<SnowflakeGrantModel> result = provider.createFrom(data);

      assertEquals(3, result.size());
      long databaseLevel =
          result.stream()
              .filter(
                  g ->
                      g.name().contains("<VIEW>")
                          && !g.name().contains("SCHEMA_A")
                          && !g.name().contains("SCHEMA_B"))
              .count();
      assertEquals(1, databaseLevel);
      long schemaA =
          result.stream().filter(g -> g.name().contains("SCHEMA_A") && g.name().contains("<VIEW>")).count();
      assertEquals(1, schemaA);
      long schemaB =
          result.stream().filter(g -> g.name().contains("SCHEMA_B") && g.name().contains("<VIEW>")).count();
      assertEquals(1, schemaB);
    }

    @Test
    @DisplayName("produces future grant with object type name format containerName dot angle-bracket type")
    void objectNameFormat() {
      ContainerGrantsCreationData data =
          containerData(
              SnowflakeObjectType.EXTERNAL_TABLE,
              "FOO_DB",
              List.of(new PolicyGrantPrivilege("SELECT")),
              "BAR_ROLE",
              false);

      List<SnowflakeGrantModel> result = provider.createFrom(data);

      assertEquals(1, result.size());
      assertTrue(result.get(0).name().startsWith("FOO_DB.<"));
      assertTrue(result.get(0).name().contains("EXTERNAL_TABLE"));
      assertEquals("EXTERNAL_TABLE", result.get(0).grantedOn());
    }
  }
}
