package us.zoom.data.dfence.providers.snowflake.revoke.factories;

import io.vavr.control.Try;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import us.zoom.data.dfence.exception.RbacDataError;
import us.zoom.data.dfence.policies.models.PolicyGrantPrivilege;
import us.zoom.data.dfence.providers.snowflake.grant.builder.SnowflakeObjectType;
import us.zoom.data.dfence.providers.snowflake.models.SnowflakeGrantModel;
import us.zoom.data.dfence.providers.snowflake.revoke.models.SnowflakeGrant;
import us.zoom.data.dfence.providers.snowflake.revoke.models.SnowflakeGrantType;
import us.zoom.data.dfence.sql.ObjectName;

import java.util.List;

@Slf4j
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class SnowflakeGrantFactory {

  public static SnowflakeGrant createFrom(SnowflakeGrantModel model) {
    return getObjectType(model.grantedOn())
        .flatMap(
            objectType ->
                getSnowflakeGrantType(model, objectType)
                    .map(grantType -> createSnowflakeGrant(model, objectType, grantType)))
        .getOrElseThrow(
            err -> {
              log.error("Failed to convert SnowflakeGrantModel to SnowflakeGrant: {}", model, err);
              return new RbacDataError("Failed to convert grant model: " + err.getMessage(), err);
            });
  }

  private static SnowflakeGrant createSnowflakeGrant(
      SnowflakeGrantModel model, SnowflakeObjectType objectType, SnowflakeGrantType grantType) {
    return new SnowflakeGrant(
                objectType, new PolicyGrantPrivilege(model.privilege()), grantType);
  }

  private static Try<SnowflakeObjectType> getObjectType(String grantedOn) {
    return Try.of(() -> SnowflakeObjectType.fromString(grantedOn));
  }

  private static Try<SnowflakeGrantType> getSnowflakeGrantType(
      SnowflakeGrantModel model, SnowflakeObjectType objectType) {
    return Try.of(() -> {
        if (model.all()) {
            throw new RbacDataError("Snowflake grant model doesn't support all: true during conversion of SnowflakeGrantModel to SnowflakeGrant");
        }
        return model.future() ? getContainerGrantType(model, objectType) :
                  getStandardGrantType(model, objectType);
    });
  }

  private static SnowflakeGrantType.Standard getStandardGrantType(
      SnowflakeGrantModel model, SnowflakeObjectType objectType) {
    List<String> parts = ObjectName.splitObjectName(model.name());
    return switch (objectType.getQualLevel()) {
      case 0 -> new SnowflakeGrantType.Standard.Global();
      case 1 -> objectType == SnowflakeObjectType.DATABASE
          ? new SnowflakeGrantType.Standard.AccountObjectDatabase(parts.get(0))
          : new SnowflakeGrantType.Standard.AccountObject(parts.get(0));
      case 2 -> new SnowflakeGrantType.Standard.Schema(parts.get(0), parts.get(1));
      case 3 -> new SnowflakeGrantType.Standard.SchemaObject(
          parts.get(0), parts.get(1), parts.get(2));
      default -> throw new RbacDataError("Unhandled qual level of object type for standard grants: " + objectType);
    };
  }

  private static SnowflakeGrantType.Container getContainerGrantType(SnowflakeGrantModel model, SnowflakeObjectType objectType) {
    List<String> parts = ObjectName.splitObjectName(model.name());
    return switch (objectType.getQualLevel()) {
      case 2 -> new SnowflakeGrantType.Container.AccountObject(parts.get(0));
      case 3 -> Try.of(() -> (SnowflakeGrantType.Container) new SnowflakeGrantType.Container.Schema(parts.get(0), parts.get(1)))
                  .getOrElse(new SnowflakeGrantType.Container.AccountObject(parts.get(0)));
      default -> throw new RbacDataError("Unhandled qual level of object type for container grants: " + objectType);
    };
  }
}
