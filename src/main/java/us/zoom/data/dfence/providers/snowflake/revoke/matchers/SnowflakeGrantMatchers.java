package us.zoom.data.dfence.providers.snowflake.revoke.matchers;

import io.vavr.collection.List;
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
    return (policyGrant, snowflakeGrant) -> {
      System.out.println("result " + matchesGrantType(snowflakeGrant.type()).test(policyGrant));

      return matchesGrantObjectType(snowflakeGrant.snowflakeObjectType())
          .and(matchesGrantPrivilege(snowflakeGrant.privilege()))
          .and(matchesGrantType(snowflakeGrant.type()))
          .test(policyGrant);
    };
  }

  public static Predicate<PolicyGrant> matchesGrantObjectType(
      SnowflakeObjectType snowflakeGrantObjectType) {
    return playbookGrant ->
        snowflakeGrantObjectType.equals(playbookGrant.objectType())
            || snowflakeGrantObjectType
                .getAliasFor()
                .equals(playbookGrant.objectType().getAliasFor());
  }

  public static Predicate<PolicyGrant> matchesGrantPrivilege(PolicyGrantPrivilege privilege) {
    return playbookGrant -> playbookGrant.privileges().stream().anyMatch(privilege::equals);
  }

  private static Predicate<PolicyGrant> matchesGrantType(SnowflakeGrantType grantType) {
    return policyGrant -> {
      System.out.println(policyGrant.policyType() + " " + grantType);
      PolicyType policyType = policyGrant.policyType();
      if (policyType instanceof PolicyType.Standard) {
        // Standard matches Standard
        return grantType instanceof SnowflakeGrantType.Standard
            && getParts(policyType).size() == getParts(grantType).size()
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

        return false;
      } else {
        return false;
      }
    };
  }

  private static boolean matchesParts(PolicyType policyType, SnowflakeGrantType grantType) {
    List<String> policyParts = getParts(policyType);
    List<String> grantParts = getParts(grantType);

    boolean a = policyParts.zip(grantParts).forAll(t -> ObjectName.equalObjectName(t._1(), t._2()));
    System.out.println("A: " + a);
    return a;
  }

  private static List<String> getParts(PolicyType policyType) {
    if (policyType instanceof PolicyType.Standard.Global) {
      return List.empty();
    } else if (policyType instanceof PolicyType.Standard.AccountObject p) {
      return List.of(p.objectName());
    } else if (policyType instanceof PolicyType.Standard.AccountObjectDatabase p) {
      return List.of(p.databaseName());
    } else if (policyType instanceof PolicyType.Standard.Schema p) {
      return List.of(p.databaseName(), p.schemaName());
    } else if (policyType instanceof PolicyType.Standard.SchemaObject p) {
      return List.of(p.databaseName(), p.schemaName(), p.objectName());
    } else if (policyType instanceof PolicyType.Container.AccountObjectDatabase p) {
      return List.of(p.databaseName());
    } else if (policyType instanceof PolicyType.Container.Schema p) {
      return List.of(p.databaseName(), p.schemaName());
    } else if (policyType instanceof PolicyType.Container.SchemaObjectAllSchemas p) {
      return List.of(p.databaseName());
    }
    return List.empty();
  }

  private static List<String> getParts(SnowflakeGrantType grantType) {
    if (grantType instanceof SnowflakeGrantType.Standard.Global) {
      return List.empty();
    } else if (grantType instanceof SnowflakeGrantType.Standard.AccountObject g) {
      return List.of(g.objectName());
    } else if (grantType instanceof SnowflakeGrantType.Standard.AccountObjectDatabase g) {
      return List.of(g.databaseName());
    } else if (grantType instanceof SnowflakeGrantType.Standard.Schema g) {
      return List.of(g.databaseName(), g.schemaName());
    } else if (grantType instanceof SnowflakeGrantType.Standard.SchemaObject g) {
      return List.of(g.databaseName(), g.schemaName(), g.objectName());
    } else if (grantType instanceof SnowflakeGrantType.Container.AccountObject g) {
      return List.of(g.objectName());
    } else if (grantType instanceof SnowflakeGrantType.Container.Schema g) {
      return List.of(g.databaseName(), g.schemaName());
    }
    return List.empty();
  }
}
