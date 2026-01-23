package us.zoom.data.dfence.providers.snowflake.grant.current;

import java.util.HashMap;
import java.util.Map;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import us.zoom.data.dfence.providers.snowflake.SnowflakeGrantsService;
import us.zoom.data.dfence.providers.snowflake.grant.builder.SnowflakeGrantBuilder;
import us.zoom.data.dfence.providers.snowflake.grant.current.models.LoadAllCurrentGrantsForRoleSettings;

@Slf4j
@AllArgsConstructor(access = AccessLevel.PUBLIC)
public class CurrentGrantsProvider {

  private final SnowflakeGrantsService snowflakeGrantsService;

  public Map<String, SnowflakeGrantBuilder> getAllCurrentGrantsForRole(
      LoadAllCurrentGrantsForRoleSettings settings) {
    if (settings.roleExists()) {
      log.debug("Role exists so we are going to get the current grants.");
      Map<String, SnowflakeGrantBuilder> currentGrantBuilders =
          this.snowflakeGrantsService.getGrants(
              settings.roleName(), !settings.revokeOtherGrants() || settings.ignoreUnknownGrants());
      log.debug("Found {} existing grants.", currentGrantBuilders.size());
      return currentGrantBuilders;
    } else {
      log.debug("Role does not exist. We will not look up existing roles.");
      return new HashMap<>();
    }
  }
}
