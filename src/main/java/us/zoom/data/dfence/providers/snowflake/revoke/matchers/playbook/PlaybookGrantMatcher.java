package us.zoom.data.dfence.providers.snowflake.revoke.matchers.playbook;

import static us.zoom.data.dfence.providers.snowflake.revoke.matchers.SnowflakeGrantMatchers.*;
import static us.zoom.data.dfence.providers.snowflake.revoke.matchers.SnowflakeGrantMatchers.grantObjectName;

import java.util.function.BiFunction;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import us.zoom.data.dfence.providers.snowflake.revoke.models.PlaybookGrant;
import us.zoom.data.dfence.providers.snowflake.revoke.models.SnowflakeGrant;

@Slf4j
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class PlaybookGrantMatcher {

  public static BiFunction<PlaybookGrant, SnowflakeGrant, Boolean> matchGrantAgainstPlaybook() {
    return (playbookGrant, snowflakeGrant) -> {
      boolean result =
          grantObjectType(snowflakeGrant.snowflakeObjectType()).apply(playbookGrant)
              && grantPrivilege(snowflakeGrant.privilege()).apply(playbookGrant)
              && grantType(snowflakeGrant.grantType()).apply(playbookGrant)
              && grantObjectName(snowflakeGrant.name()).apply(playbookGrant);

      if (!result) {
        log.info(
            "Playbook grant {} match failed for snowflake grant {}", playbookGrant, snowflakeGrant);
      } else {
        log.info("Playbook grant {} matched snowflake grant {}", playbookGrant, snowflakeGrant);
      }
      return result;
    };
  }
}
