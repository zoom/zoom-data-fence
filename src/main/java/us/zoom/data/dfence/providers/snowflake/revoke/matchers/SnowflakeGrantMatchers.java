package us.zoom.data.dfence.providers.snowflake.revoke.matchers;

import java.util.function.BiPredicate;
import java.util.function.Predicate;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import us.zoom.data.dfence.policies.models.PolicyGrant;
import us.zoom.data.dfence.policies.models.PolicyGrantPrivilege;
import us.zoom.data.dfence.policies.pattern.models.PolicyType;
import us.zoom.data.dfence.providers.snowflake.grant.builder.SnowflakeObjectType;
import us.zoom.data.dfence.providers.snowflake.revoke.models.SnowflakeGrant;
import us.zoom.data.dfence.providers.snowflake.revoke.models.SnowflakeGrantType;
import us.zoom.data.dfence.sql.ObjectName;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class SnowflakeGrantMatchers {

  public static BiPredicate<PolicyGrant, SnowflakeGrant> matchesSnowflakeGrant() {
    return (policyGrant, snowflakeGrant) ->
        matchesGrantAlias(snowflakeGrant.snowflakeObjectType())
            .and(matchesGrantPrivilege(snowflakeGrant.privilege()))
            .and(matchesGrantType(snowflakeGrant.type()))
            .test(policyGrant);
  }

  public static Predicate<PolicyGrant> matchesGrantAlias(
      SnowflakeObjectType snowflakeGrantObjectType) {
    return policyGrant ->
            policyGrant.objectType().getAliasFor().equals(
                            snowflakeGrantObjectType
                                    .getAliasFor()
                    );
  }

  public static Predicate<PolicyGrant> matchesGrantPrivilege(PolicyGrantPrivilege privilege) {
    return playbookGrant -> playbookGrant.privileges().stream().anyMatch(privilege::equals);
  }

  private static Predicate<PolicyGrant> matchesGrantType(SnowflakeGrantType grantType) {
    return policyGrant -> {
      PolicyType policyType = policyGrant.policyType();
      if (policyType instanceof PolicyType.Standard) {
        return grantType instanceof SnowflakeGrantType.Standard
            && policyType.parts().size() == grantType.parts().size()
            && matchesParts(policyGrant.policyType(), grantType);
      } else if (policyType instanceof PolicyType.Container c) {
        if (c.containerPolicyOptions().all()) {
          if (grantType instanceof SnowflakeGrantType.Standard
              && matchesParts(policyGrant.policyType(), grantType)) return true;
        }
        if (c.containerPolicyOptions().future()) {
          return (grantType instanceof SnowflakeGrantType.Standard
                  || grantType instanceof SnowflakeGrantType.Container)
              && matchesParts(policyGrant.policyType(), grantType);
        }
      }
      return false;
    };
  }

  private static boolean matchesParts(PolicyType policyType, SnowflakeGrantType grantType) {
    return policyType.parts().zip(grantType.parts())
            .forAll(t -> ObjectName.equalObjectName(t._1(), t._2()));
  }
}
