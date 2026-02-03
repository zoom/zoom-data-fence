package us.zoom.data.dfence.providers.snowflake.grant.desired.create.internal;

import java.util.List;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import us.zoom.data.dfence.providers.snowflake.grant.desired.create.data.models.GrantsCreationData;
import us.zoom.data.dfence.providers.snowflake.models.SnowflakeGrantModel;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class StandardGrantsCompiler {

  public static List<SnowflakeGrantModel> createFrom(GrantsCreationData.Standard data) {
    return data.privileges().stream()
        .map(
            p ->
                new SnowflakeGrantModel(
                    p.value(),
                    data.objectType().getObjectType().replace(" ", "_"),
                    data.normalizedObjectName(),
                    "ROLE",
                    data.roleName(),
                    false,
                    false,
                    false))
        .toList();
  }
}
