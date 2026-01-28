package us.zoom.data.dfence.providers.snowflake.grant.desired.create.internal;

import java.util.List;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import us.zoom.data.dfence.providers.snowflake.grant.desired.create.data.models.GrantsCreationData;
import us.zoom.data.dfence.providers.snowflake.models.SnowflakeGrantModel;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class StandardGrantsProvider {

  public static List<SnowflakeGrantModel> createGrants(GrantsCreationData.Standard plan) {
    return plan.privileges().stream()
        .map(
            p ->
                new SnowflakeGrantModel(
                    p.value(),
                    plan.objectType().getObjectType().replace(" ", "_"),
                    plan.normalizedObjectName(),
                    "ROLE",
                    plan.roleName(),
                    false,
                    false,
                    false))
        .toList();
  }
}
