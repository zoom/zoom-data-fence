package us.zoom.data.dfence.providers.snowflake.revoke.matchers.playbook;

import static us.zoom.data.dfence.providers.snowflake.revoke.matchers.SnowflakeGrantMatchers.*;

import java.util.function.BiPredicate;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import us.zoom.data.dfence.providers.snowflake.revoke.models.PlaybookGrant;
import us.zoom.data.dfence.providers.snowflake.revoke.models.SnowflakeGrant;

@Slf4j
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class PlaybookGrantMatcher {

  /**
   * Creates a predicate that matches a SnowflakeGrant against a PlaybookGrant. Checks object type,
   * privilege, grant type, and object name.
   */
  public static BiPredicate<PlaybookGrant, SnowflakeGrant> matchGrantAgainstPlaybook() {
    return (playbookGrant, snowflakeGrant) ->
        matchesGrantObjectType(snowflakeGrant.snowflakeObjectType())
            .and(matchesGrantPrivilege(snowflakeGrant.privilege()))
            .and(matchesGrantType(snowflakeGrant.grantType()))
            .and(matchesGrantName(snowflakeGrant.name(), snowflakeGrant.snowflakeObjectType()))
            .test(playbookGrant);
  }
}
