package us.zoom.data.dfence.providers.snowflake.revoke.companions;

import io.vavr.control.Try;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import us.zoom.data.dfence.exception.RbacDataError;
import us.zoom.data.dfence.providers.snowflake.grant.builder.SnowflakeObjectType;
import us.zoom.data.dfence.providers.snowflake.models.SnowflakeGrantModel;
import us.zoom.data.dfence.providers.snowflake.revoke.models.SnowflakeGrant;
import us.zoom.data.dfence.providers.snowflake.revoke.models.SnowflakeGrantType;
import us.zoom.data.dfence.providers.snowflake.revoke.models.wrappers.GrantPrivilege;
import us.zoom.data.dfence.providers.snowflake.revoke.models.wrappers.SnowflakeGrantObjectName;

@Slf4j
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class SnowflakeGrantCompanion {

  public static SnowflakeGrant from(SnowflakeGrantModel model) {
    return getSnowflakeGrantType(model.future(), model.all())
        .flatMap(grantType -> createSnowflakeGrant(model, grantType))
        .getOrElseThrow(
            err -> {
              log.error("Failed to convert SnowflakeGrantModel to SnowflakeGrant: {}", model, err);
              return new RbacDataError("Failed to convert grant model: " + err.getMessage(), err);
            });
  }

  private static Try<SnowflakeGrant> createSnowflakeGrant(
      SnowflakeGrantModel model, SnowflakeGrantType grantType) {
    return getObjectType(model.grantedOn())
        .map(
            objectType ->
                new SnowflakeGrant(
                    objectType,
                    new GrantPrivilege(model.privilege()),
                    grantType,
                    SnowflakeGrantObjectName.apply(model.name())));
  }

  private static Try<SnowflakeObjectType> getObjectType(String grantedOn) {
    return Try.of(() -> SnowflakeObjectType.fromString(grantedOn));
  }

  private static Try<SnowflakeGrantType> getSnowflakeGrantType(boolean future, boolean all) {
    return Try.of(
        () -> {
          if (future && all) {
            throw new RbacDataError(
                "Snowflake grant cannot have both future and all flags set to true");
          }
          if (future) return SnowflakeGrantType.FUTURE;
          if (all) return SnowflakeGrantType.ALL;
          return SnowflakeGrantType.STANDARD;
        });
  }
}
