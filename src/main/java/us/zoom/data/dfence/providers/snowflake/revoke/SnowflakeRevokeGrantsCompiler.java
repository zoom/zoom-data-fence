package us.zoom.data.dfence.providers.snowflake.revoke;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import us.zoom.data.dfence.playbook.model.PlaybookPrivilegeGrant;
import us.zoom.data.dfence.providers.snowflake.grant.builder.SnowflakeGrantBuilder;
import us.zoom.data.dfence.providers.snowflake.revoke.evaluator.GrantRevocationEvaluator;
import us.zoom.data.dfence.providers.snowflake.revoke.index.PlaybookGrantHashIndexer;
import us.zoom.data.dfence.providers.snowflake.revoke.models.PlaybookGrantHashIndex;

@Slf4j
public class SnowflakeRevokeGrantsCompiler {

  public List<SnowflakeGrantBuilder> compileRevokeGrants(
      List<PlaybookPrivilegeGrant> privilegeGrants,
      Map<String, SnowflakeGrantBuilder> currentGrantBuilders) {
    PlaybookGrantHashIndex playbookGrantsIndex = PlaybookGrantHashIndexer.create(privilegeGrants);
    GrantRevocationEvaluator evaluator = new GrantRevocationEvaluator(playbookGrantsIndex);

    return currentGrantBuilders.values().parallelStream()
        .filter(grantBuilder -> evaluator.needsRevoke(grantBuilder.getGrant()))
        .sorted(Comparator.comparing(SnowflakeGrantBuilder::getKey))
        .collect(Collectors.toList());
  }
}
