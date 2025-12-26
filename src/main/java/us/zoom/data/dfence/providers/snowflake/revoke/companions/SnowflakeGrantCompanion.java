package us.zoom.data.dfence.providers.snowflake.revoke.companions;

import java.util.Optional;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import us.zoom.data.dfence.providers.snowflake.grant.builder.SnowflakeObjectType;
import us.zoom.data.dfence.providers.snowflake.models.SnowflakeGrantModel;
import us.zoom.data.dfence.providers.snowflake.revoke.models.SnowflakeGrant;
import us.zoom.data.dfence.providers.snowflake.revoke.models.SnowflakeGrantType;
import us.zoom.data.dfence.providers.snowflake.revoke.models.wrappers.GrantPrivilege;
import us.zoom.data.dfence.providers.snowflake.revoke.models.wrappers.SnowflakeGrantObjectName;

@Slf4j
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class SnowflakeGrantCompanion {

  public static Optional<SnowflakeGrant> from(SnowflakeGrantModel model) {
    try {
      SnowflakeGrantType grantType = getSnowflakeGrantType(model.future(), model.all());

      return Optional.of(
          new SnowflakeGrant(
              SnowflakeObjectType.fromString(model.grantedOn()),
              new GrantPrivilege(model.privilege()),
              grantType,
              SnowflakeGrantObjectName.apply(model.name())));
    } catch (Exception e) {
      log.error("Conversion to SnowflakeGrant failed for grant model {}", model, e);
      return Optional.empty();
    }
  }

  private static SnowflakeGrantType getSnowflakeGrantType(boolean future, boolean all) {
    if (future && !all) return SnowflakeGrantType.Future;
    if (!future && all) return SnowflakeGrantType.All;
    return SnowflakeGrantType.Standard;
  }
}
