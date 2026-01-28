package us.zoom.data.dfence.providers.snowflake.grant.diff;

import com.google.common.collect.MapDifference;
import com.google.common.collect.Maps;
import java.util.Map;
import java.util.concurrent.ForkJoinPool;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import us.zoom.data.dfence.providers.snowflake.grant.builder.SnowflakeGrantBuilder;
import us.zoom.data.dfence.providers.snowflake.grant.desired.create.DesiredGrantsProvider;
import us.zoom.data.dfence.providers.snowflake.grant.diff.models.AllDesiredGrantsCreationDataForRole;

@Slf4j
public class GrantsDiffProvider {
  private final ForkJoinPool forkJoinPool;
  private final DesiredGrantsProvider desiredGrantsProvider;

  public GrantsDiffProvider(
      DesiredGrantsProvider desiredGrantsProvider, ForkJoinPool forkJoinPool) {
    this.forkJoinPool = forkJoinPool;
    this.desiredGrantsProvider = desiredGrantsProvider;
  }

  public MapDifference<String, SnowflakeGrantBuilder> diff(
      AllDesiredGrantsCreationDataForRole data,
      Map<String, SnowflakeGrantBuilder> allCurrentGrantsForRole) {
    return Maps.difference(allCurrentGrantsForRole, getAllDesiredGrantsForRole(data));
  }

  private Map<String, SnowflakeGrantBuilder> getAllDesiredGrantsForRole(
      AllDesiredGrantsCreationDataForRole data) {
    return forkJoinPool
        .submit(
            () ->
                data.privilegeGrants().parallelStream()
                    .flatMap(
                        x ->
                            desiredGrantsProvider
                                .playbookGrantToSnowflakeGrants(x, data.roleName(), data.options())
                                .stream())
                    .collect(
                        Collectors.toMap(SnowflakeGrantBuilder::getKey, x -> x, (x0, x1) -> x0)))
        .join();
  }
}
