package us.zoom.data.dfence.providers.snowflake.revoke.matchers;

import static us.zoom.data.dfence.providers.snowflake.revoke.matchers.PlaybookPatternMatchers.*;

import java.util.List;
import java.util.function.Function;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import us.zoom.data.dfence.providers.snowflake.grant.builder.SnowflakeObjectType;
import us.zoom.data.dfence.providers.snowflake.revoke.collection.NonEmptyList;
import us.zoom.data.dfence.providers.snowflake.revoke.models.PlaybookGrant;
import us.zoom.data.dfence.providers.snowflake.revoke.models.PlaybookPattern;
import us.zoom.data.dfence.providers.snowflake.revoke.models.SnowflakeGrantType;
import us.zoom.data.dfence.providers.snowflake.revoke.models.wrappers.GrantPrivilege;
import us.zoom.data.dfence.providers.snowflake.revoke.models.wrappers.SnowflakeGrantObjectName;
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
        log.info(
            "Playbook grant {} match failed for snowflake grant grant-object-type {}",
            playbookGrant,
            snowflakeGrantObjectType);
      }
      return result;
    };
  }

  public static Function<PlaybookGrant, Boolean> grantPrivilege(GrantPrivilege privilege) {
    return playbookGrant -> {
      NonEmptyList<GrantPrivilege> privileges = playbookGrant.privileges();
      boolean result = privileges.asImmutableList().contains(privilege);
      if (!result) {
        log.info(
            "Playbook grant {} match failed for snowflake grant grant-privilege {}",
            playbookGrant,
            privilege);
      }
      return result;
    };
  }

  public static Function<PlaybookGrant, Boolean> grantType(SnowflakeGrantType snowflakeGrantType) {
    return playbookGrant -> {
      NonEmptyList<SnowflakeGrantType> playbookTypes = playbookGrant.grantTypes();
      boolean result =
          playbookTypes.asImmutableList().stream()
              .anyMatch(playbookType -> checkGrantType(playbookType, snowflakeGrantType));
      if (!result) {
        log.info(
            "Playbook grant {} match failed for snowflake grant grant-type {}",
            playbookGrant,
            snowflakeGrantType);
      }
      return result;
    };
  }

  private static boolean checkGrantType(
      SnowflakeGrantType playbookType, SnowflakeGrantType snowflakeGrantType) {
    return switch (playbookType) {
      case Future ->
          snowflakeGrantType == SnowflakeGrantType.Future
              || snowflakeGrantType == SnowflakeGrantType.Standard;
      case All ->
          snowflakeGrantType == SnowflakeGrantType.All
              || snowflakeGrantType == SnowflakeGrantType.Standard;
      case Standard -> snowflakeGrantType == SnowflakeGrantType.Standard;
    };
  }

  public static Function<PlaybookGrant, Boolean> grantObjectName(
      SnowflakeGrantObjectName snowflakeGrantObjectName) {
    return playbookGrant -> {
      PlaybookPattern pattern = playbookGrant.pattern();
      List<String> grantObjectNameParts =
          ObjectName.splitObjectName(snowflakeGrantObjectName.value());
      boolean result =
          switch (grantObjectNameParts.size()) {
            case 1 ->
                accountLevelObject(grantObjectNameParts.get(0)).apply(pattern)
                    || accountLevelGrant(grantObjectNameParts.get(0)).apply(pattern)
                    || database(grantObjectNameParts.get(0)).apply(pattern);
            case 2 ->
                database(grantObjectNameParts.get(0)).apply(pattern)
                    && schema(grantObjectNameParts.get(1)).apply(pattern);
            case 3 ->
                database(grantObjectNameParts.get(0)).apply(pattern)
                    && schema(grantObjectNameParts.get(1)).apply(pattern)
                    && object(grantObjectNameParts.get(2)).apply(pattern);
            default -> false;
          };

      if (!result) {
        log.info(
            "Playbook grant {} match failed for snowflake grant grant-object-name {}",
            playbookGrant,
            snowflakeGrantObjectName);
      }
      return result;
    };
  }
}
