package us.zoom.data.dfence.providers.snowflake.revoke.matchers;

import static us.zoom.data.dfence.providers.snowflake.revoke.matchers.PlaybookPatternMatchers.*;

import java.util.List;
import java.util.function.Function;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import us.zoom.data.dfence.providers.snowflake.grant.builder.SnowflakeObjectType;
import us.zoom.data.dfence.providers.snowflake.revoke.models.PlaybookGrant;
import us.zoom.data.dfence.providers.snowflake.revoke.models.PlaybookGrantType;
import us.zoom.data.dfence.providers.snowflake.revoke.models.PlaybookPattern;
import us.zoom.data.dfence.providers.snowflake.revoke.models.SnowflakeGrantType;
import us.zoom.data.dfence.providers.snowflake.revoke.models.wrappers.GrantPrivilege;
import us.zoom.data.dfence.providers.snowflake.revoke.models.wrappers.SnowflakeGrantName;
import us.zoom.data.dfence.sql.ObjectName;

@Slf4j
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class SnowflakeGrantMatchers {

  public static Function<PlaybookGrant, Boolean> grantObjectType(
      SnowflakeObjectType snowflakeGrantObjectType) {
    return playbookGrant -> {
      boolean result =
          snowflakeGrantObjectType == playbookGrant.objectType()
              || snowflakeGrantObjectType
                  .getAliasFor()
                  .equals(playbookGrant.objectType().getAliasFor());
      if (!result) {
        log.debug(
            "Playbook grant {} match failed for snowflake grant grant-object-type {}",
            playbookGrant,
            snowflakeGrantObjectType);
      }
      return result;
    };
  }

  public static Function<PlaybookGrant, Boolean> grantPrivilege(GrantPrivilege privilege) {
    return playbookGrant -> {
      boolean result = playbookGrant.privileges().contains(privilege);
      if (!result) {
        log.debug(
            "Playbook grant {} match failed for snowflake grant grant-privilege {}",
            playbookGrant,
            privilege);
      }
      return result;
    };
  }

  public static Function<PlaybookGrant, Boolean> grantType(SnowflakeGrantType snowflakeGrantType) {
    return playbookGrant -> {
      PlaybookGrantType playbookGrantType = playbookGrant.grantType();
      boolean result = checkGrantType(playbookGrantType, snowflakeGrantType);
      if (!result) {
        log.debug(
            "Playbook grant {} match failed for snowflake grant grant-type {}",
            playbookGrant,
            snowflakeGrantType);
      }
      return result;
    };
  }

  private static boolean checkGrantType(
      PlaybookGrantType playbookGrantType, SnowflakeGrantType snowflakeGrantType) {
    return switch (playbookGrantType) {
      case FUTURE ->
          snowflakeGrantType == SnowflakeGrantType.Future
              || snowflakeGrantType == SnowflakeGrantType.Standard;
      case ALL ->
          snowflakeGrantType == SnowflakeGrantType.All
              || snowflakeGrantType == SnowflakeGrantType.Standard;
      case STANDARD -> snowflakeGrantType == SnowflakeGrantType.Standard;
      case FUTURE_AND_ALL ->
          snowflakeGrantType == SnowflakeGrantType.Future
              || snowflakeGrantType == SnowflakeGrantType.All
              || snowflakeGrantType == SnowflakeGrantType.Standard;
    };
  }

  public static Function<PlaybookGrant, Boolean> grantName(SnowflakeGrantName snowflakeGrantName) {
    return playbookGrant -> {
      PlaybookPattern pattern = playbookGrant.pattern();
      List<String> grantObjectNameParts = ObjectName.splitObjectName(snowflakeGrantName.value());

      boolean result =
          switch (grantObjectNameParts.size()) {
            case 1 -> // qual level 0 and qual level 1 is handled in this case
                accountLevelObject(grantObjectNameParts.get(0)).apply(pattern)
                    || accountLevel(grantObjectNameParts.get(0)).apply(pattern)
                    || database(grantObjectNameParts.get(0)).apply(pattern);
            case 2 ->
                database(grantObjectNameParts.get(0)).apply(pattern)
                    && object(grantObjectNameParts.get(1)).apply(pattern);
            case 3 ->
                database(grantObjectNameParts.get(0)).apply(pattern)
                    && schema(grantObjectNameParts.get(1)).apply(pattern)
                    && object(grantObjectNameParts.get(2)).apply(pattern);
            default -> false;
          };

      if (!result) {
        log.debug(
            "Playbook grant {} match failed for snowflake grant grant-name {}",
            playbookGrant,
            snowflakeGrantName);
      }
      return result;
    };
  }
}
