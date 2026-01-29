package us.zoom.data.dfence.providers.snowflake.grant.create.internal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import us.zoom.data.dfence.providers.snowflake.grant.builder.SnowflakeObjectType;
import us.zoom.data.dfence.providers.snowflake.grant.desired.create.data.models.GrantsCreationData;
import us.zoom.data.dfence.providers.snowflake.grant.desired.create.internal.StandardGrantsProvider;
import us.zoom.data.dfence.providers.snowflake.models.SnowflakeGrantModel;
import us.zoom.data.dfence.policies.models.PolicyGrantPrivilege;

@DisplayName("StandardGrantsProvider")
class StandardGrantsProviderTest {

  @Nested
  @DisplayName("createGrants")
  class CreateGrants {

    @Test
    @DisplayName("returns one grant per privilege with correct privilege and role")
    void oneGrantPerPrivilege() {
      GrantsCreationData.Standard data =
          new GrantsCreationData.Standard(
              SnowflakeObjectType.TABLE,
              "MY_DB.MY_SCHEMA.MY_TABLE",
              List.of(new PolicyGrantPrivilege("SELECT"), new PolicyGrantPrivilege("UPDATE")),
              "MY_ROLE");

      List<SnowflakeGrantModel> result = StandardGrantsProvider.createGrants(data);

      assertEquals(2, result.size());
      SnowflakeGrantModel selectGrant = result.get(0);
      assertEquals("SELECT", selectGrant.privilege());
      assertEquals("TABLE", selectGrant.grantedOn());
      assertEquals("MY_ROLE", selectGrant.granteeName());
      assertFalse(selectGrant.future());
      assertFalse(selectGrant.all());

      SnowflakeGrantModel updateGrant = result.get(1);
      assertEquals("UPDATE", updateGrant.privilege());
      assertEquals("TABLE", updateGrant.grantedOn());
    }

    @Test
    @DisplayName("uses normalized object name and ROLE as grantedTo")
    void objectNameAndGrantedTo() {
      GrantsCreationData.Standard data =
          new GrantsCreationData.Standard(
              SnowflakeObjectType.TABLE,
              "MY_DB.MY_SCHEMA.MY_TABLE",
              List.of(new PolicyGrantPrivilege("SELECT")),
              "MY_ROLE");

      List<SnowflakeGrantModel> result = StandardGrantsProvider.createGrants(data);

      assertEquals(1, result.size());
      assertEquals("MY_DB.MY_SCHEMA.MY_TABLE", result.get(0).name());
      assertEquals("ROLE", result.get(0).grantedTo());
    }

    @Test
    @DisplayName("replaces spaces in object type with underscore for grantedOn")
    void objectTypeWithSpaceNormalizedToUnderscore() {
      GrantsCreationData.Standard data =
          new GrantsCreationData.Standard(
              SnowflakeObjectType.EXTERNAL_TABLE,
              "MY_DB.MY_SCHEMA.MY_TABLE",
              List.of(new PolicyGrantPrivilege("SELECT")),
              "MY_ROLE");

      List<SnowflakeGrantModel> result = StandardGrantsProvider.createGrants(data);

      assertEquals(1, result.size());
      assertEquals("EXTERNAL_TABLE", result.get(0).grantedOn());
    }

    @Test
    @DisplayName("returns empty list when no privileges")
    void emptyPrivilegesReturnsEmptyList() {
      GrantsCreationData.Standard data =
          new GrantsCreationData.Standard(
              SnowflakeObjectType.TABLE,
              "MY_DB.MY_SCHEMA.MY_TABLE",
              List.of(),
              "MY_ROLE");

      List<SnowflakeGrantModel> result = StandardGrantsProvider.createGrants(data);

      assertEquals(0, result.size());
    }
  }
}
