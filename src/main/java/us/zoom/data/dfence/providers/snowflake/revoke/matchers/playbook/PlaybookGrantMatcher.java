package us.zoom.data.dfence.providers.snowflake.revoke.matchers.playbook;

import java.util.function.BiFunction;
import us.zoom.data.dfence.providers.snowflake.revoke.models.PlaybookGrant;
import us.zoom.data.dfence.providers.snowflake.revoke.models.SnowflakeGrant;

public interface PlaybookGrantMatcher {
  BiFunction<PlaybookGrant, SnowflakeGrant, Boolean> matchGrantAgainstPlaybook();

  static PlaybookGrantMatcher apply() {
    return new PlaybookGrantMatcherLive();
  }
}
