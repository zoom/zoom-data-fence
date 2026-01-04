package us.zoom.data.dfence.providers.snowflake.revoke.matchers;

import java.util.Optional;
import java.util.function.Function;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import us.zoom.data.dfence.providers.snowflake.revoke.models.PlaybookPattern;
import us.zoom.data.dfence.sql.ObjectName;

@Slf4j
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class PlaybookPatternMatchers {

  public static Function<PlaybookPattern, Boolean> accountLevel(String value) {
    return pattern -> {
      boolean result = value.trim().isEmpty();
      if (!result) {
        log.debug("Playbook pattern {} match failed for account-level value {}", pattern, value);
      }
      return result;
    };
  }

  public static Function<PlaybookPattern, Boolean> accountLevelObject(String snowObj) {
    return pattern -> {
      Optional<String> objName = pattern.objName();
      boolean result =
          pattern.dbName().isEmpty()
              && pattern.schName().isEmpty()
              && objName.isPresent()
              && !"*".equals(objName.get())
              && ObjectName.equalObjectName(snowObj.trim(), objName.get());
      if (!result) {
        log.debug(
            "Playbook pattern {} match failed for account-level-object value {}", pattern, snowObj);
      }
      return result;
    };
  }

  public static Function<PlaybookPattern, Boolean> database(String snowDb) {
    return pattern -> {
      Optional<String> dbName = pattern.dbName();
      boolean result =
          dbName.isPresent()
              && !"*".equals(dbName.get())
              && ObjectName.equalObjectName(snowDb.trim(), dbName.get().trim());
      if (!result) {
        log.debug("Playbook pattern {} match failed for database value {}", pattern, snowDb);
      }
      return result;
    };
  }

  public static Function<PlaybookPattern, Boolean> schema(String snowSchema) {
    return pattern -> {
      Optional<String> schName = pattern.schName();
      boolean result =
          schName.isEmpty()
              || "*".equals(schName.orElse(""))
              || ObjectName.equalObjectName(snowSchema.trim(), schName.get().trim());
      if (!result) {
        log.debug("Playbook pattern {} match failed for schema value {}", pattern, snowSchema);
      }
      return result;
    };
  }

  public static Function<PlaybookPattern, Boolean> object(String snowObj) {
    return pattern -> {
      Optional<String> objName = pattern.objName();
      boolean result =
          objName.isEmpty()
              || "*".equals(objName.orElse(""))
              || ObjectName.equalObjectName(snowObj.trim(), objName.get().trim());
      if (!result) {
        log.debug("Playbook pattern {} match failed for object value {}", pattern, snowObj);
      }
      return result;
    };
  }
}
