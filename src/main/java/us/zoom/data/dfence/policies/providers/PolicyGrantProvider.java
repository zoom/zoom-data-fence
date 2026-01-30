package us.zoom.data.dfence.policies.providers;

import io.vavr.control.Option;
import io.vavr.control.Try;
import java.util.List;
import java.util.stream.Collectors;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import us.zoom.data.dfence.exception.RbacDataError;
import us.zoom.data.dfence.playbook.model.PlaybookPrivilegeGrant;
import us.zoom.data.dfence.providers.snowflake.grant.builder.SnowflakeObjectType;
import us.zoom.data.dfence.policies.pattern.providers.PolicyTypeProvider;
import us.zoom.data.dfence.policies.pattern.models.PolicyType;
import us.zoom.data.dfence.policies.pattern.models.ValidationError;
import us.zoom.data.dfence.policies.models.PolicyGrantPrivilege;
import us.zoom.data.dfence.policies.models.PolicyGrant;
import us.zoom.data.dfence.policies.models.PolicyPattern;
import us.zoom.data.dfence.policies.models.PolicyPatternOptions;

@Slf4j
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class PolicyGrantProvider {

  public static PolicyGrant getPolicyGrant(PlaybookPrivilegeGrant grant) {
    return getPlaybookGrant(grant)
        .getOrElseThrow(
            err -> {
              log.error(
                  "Failed to convert playbook privilege grant to playbook grant: {}", grant, err);
              return new RbacDataError(
                  "Failed to convert playbook privilege grant: " + err.getMessage(), err);
            });
  }

  private static Try<PolicyGrant> getPlaybookGrant(PlaybookPrivilegeGrant grant) {
    return getSnowflakeObjectType(grant.objectType())
        .flatMap(snowflakeObjectType -> getPlaybookGrant(snowflakeObjectType, grant));
  }

  private static Try<PolicyGrant> getPlaybookGrant(
      SnowflakeObjectType snowflakeObjectType, PlaybookPrivilegeGrant grant) {
    return Try.of(
        () -> {
          Option<String> dbName =
              Option.of(grant.databaseName()).map(String::trim).filter(s -> !s.isEmpty());
          Option<String> schName =
              Option.of(grant.schemaName()).map(String::trim).filter(s -> !s.isEmpty());
          Option<String> objName =
              Option.of(grant.objectName()).map(String::trim).filter(s -> !s.isEmpty());

          PolicyPattern pattern = PolicyPattern.of(dbName, schName, objName);
          PolicyPatternOptions options =
              new PolicyPatternOptions(grant.includeFuture(), grant.includeAll());
          PolicyType policyType =
                  createPolicyType(pattern, snowflakeObjectType, options);

          List<PolicyGrantPrivilege> privileges =
              grant.privileges().stream().map(PolicyGrantPrivilege::new).collect(Collectors.toList());

          return new PolicyGrant(
              snowflakeObjectType, privileges, policyType, grant.enable());
        });
  }

  private static Try<SnowflakeObjectType> getSnowflakeObjectType(String grantObjectType) {
    return Try.of(() -> SnowflakeObjectType.fromString(grantObjectType));
  }

  private static PolicyType createPolicyType(
      PolicyPattern pattern,
      SnowflakeObjectType snowflakeObjectType,
      PolicyPatternOptions options) {
    return Try.of(
            () -> PolicyTypeProvider.getPolicyType(pattern, snowflakeObjectType, options))
        .flatMap(
            validatedPattern ->
                validatedPattern.fold(
                    errors ->
                        Try.failure(
                            new RbacDataError(
                                "Playbook pattern validation failed: "
                                    + errors
                                        .map(ValidationError::message)
                                        .mkString("[", ", ", "]"))),
                    Try::success))
        .getOrElseThrow(e -> new RbacDataError("Pattern validation failed: " + e.getMessage(), e));
  }
}
