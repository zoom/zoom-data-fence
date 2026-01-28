package us.zoom.data.dfence.providers.snowflake.revoke.matchers;

import static us.zoom.data.dfence.providers.snowflake.revoke.matchers.PlaybookPatternMatchers.*;

import io.vavr.collection.List;
import io.vavr.control.Try;
import java.util.function.Predicate;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import us.zoom.data.dfence.providers.snowflake.grant.builder.SnowflakeObjectType;
import us.zoom.data.dfence.providers.snowflake.revoke.models.PlaybookGrant;
import us.zoom.data.dfence.providers.snowflake.revoke.models.PlaybookPattern;
import us.zoom.data.dfence.providers.snowflake.revoke.models.SnowflakeGrantType;
import us.zoom.data.dfence.providers.snowflake.revoke.models.wrappers.GrantPrivilege;
import us.zoom.data.dfence.providers.snowflake.revoke.models.wrappers.SnowflakeGrantObjectName;
import us.zoom.data.dfence.sql.ObjectName;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class SnowflakeGrantMatchers {

  public static Predicate<PlaybookGrant> matchesGrantObjectType(
      SnowflakeObjectType snowflakeGrantObjectType) {
    return playbookGrant ->
        snowflakeGrantObjectType.equals(playbookGrant.objectType())
            || snowflakeGrantObjectType
                .getAliasFor()
                .equals(playbookGrant.objectType().getAliasFor());
  }

  public static Predicate<PlaybookGrant> matchesGrantPrivilege(GrantPrivilege privilege) {
    return playbookGrant -> playbookGrant.privileges().stream().anyMatch(privilege::equals);
  }

  public static Predicate<PlaybookGrant> matchesGrantType(SnowflakeGrantType snowflakeGrantType) {
    return playbookGrant -> {
      Predicate<SnowflakeGrantType> matchesStandard =
          objectType -> objectType == SnowflakeGrantType.STANDARD;
      Predicate<SnowflakeGrantType> matchesFuture =
          objectType -> objectType == SnowflakeGrantType.FUTURE;
      Predicate<SnowflakeGrantType> matchesAll = objectType -> objectType == SnowflakeGrantType.ALL;

      Predicate<SnowflakeGrantType> matchesEverything =
          matchesStandard.or(matchesFuture).or(matchesAll);

      Predicate<SnowflakeGrantType> result =
          switch (playbookGrant.resolvedPattern().playbookPatternOptions()) {
            case STANDARD -> matchesStandard;
            case FUTURE -> matchesFuture;
            case ALL -> matchesStandard.or(matchesAll);
            case FUTURE_AND_ALL -> matchesEverything;
          };
      return result.test(snowflakeGrantType);
    };
  }

  /**
   * Creates a predicate that matches playbook grants by object name. Matching depends on object
   * type qualification level (0-3).
   */
  public static Predicate<PlaybookGrant> matchesGrantName(
      SnowflakeGrantObjectName grantName, SnowflakeObjectType snowflakeObjectType) {
    return playbookGrant -> {
      PlaybookPattern pattern = playbookGrant.pattern();
      List<String> grantObjectNameParts = List.ofAll(ObjectName.splitObjectName(grantName.value()));
      int qualLevel = snowflakeObjectType.getQualLevel();

      Predicate<PlaybookPattern> result =
          switch (qualLevel) {
            case 0 -> PlaybookPatternMatchers.global();
            case 1 -> Try.of(() -> grantObjectNameParts.get(0))
                .map(value -> accountObject(value).or(database(value)))
                .getOrElse(noMatch());
            case 2 -> {
              Predicate<PlaybookPattern> a =
                  Try.of(() -> grantObjectNameParts.get(0))
                      .map(PlaybookPatternMatchers::database)
                      .getOrElse(noMatch());

              Predicate<PlaybookPattern> b =
                  Try.of(() -> grantObjectNameParts.get(1))
                      .map(
                          value -> {
                            Predicate<PlaybookPattern> c =
                                schema(value).or(sch().isEmptyOrWildcard());

                            Predicate<PlaybookPattern> d =
                                object(value).or(obj().isEmptyOrWildcard());

                            return c.or(d);
                          })
                      .getOrElse(sch().isEmptyOrWildcard().or(obj().isEmptyOrWildcard()));

              yield a.and(b);
            }
            case 3 -> {
              Predicate<PlaybookPattern> a =
                  Try.of(() -> grantObjectNameParts.get(0))
                      .map(PlaybookPatternMatchers::database)
                      .getOrElse(noMatch());

              Predicate<PlaybookPattern> b =
                  Try.of(() -> grantObjectNameParts.get(1))
                      .map(value -> schema(value).or(sch().isEmptyOrWildcard()))
                      .getOrElse(sch().isEmptyOrWildcard());

              Predicate<PlaybookPattern> c =
                  Try.of(() -> grantObjectNameParts.get(2))
                      .map(value -> object(value).or(obj().isEmptyOrWildcard()))
                      .getOrElse(obj().isEmptyOrWildcard());

              yield a.and(b.and(c));
            }
            default -> noMatch();
          };

      return result.test(pattern);
    };
  }
}
