package us.zoom.data.dfence.providers.snowflake.grant.create.validations.playbook.pattern.providers;

import com.google.common.collect.ImmutableList;
import io.vavr.collection.Seq;
import io.vavr.control.Option;
import io.vavr.control.Validation;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import us.zoom.data.dfence.providers.snowflake.grant.builder.SnowflakeObjectType;
import us.zoom.data.dfence.policies.pattern.providers.PolicyTypeProvider;
import us.zoom.data.dfence.policies.pattern.models.ContainerPolicyOption;
import us.zoom.data.dfence.policies.pattern.models.ContainerPolicyOptions;
import us.zoom.data.dfence.policies.pattern.models.PolicyType;
import us.zoom.data.dfence.policies.pattern.models.ValidationError;
import us.zoom.data.dfence.policies.models.PolicyGrantPrivilege;
import us.zoom.data.dfence.policies.models.PolicyGrant;
import us.zoom.data.dfence.policies.models.PolicyPattern;
import us.zoom.data.dfence.policies.models.PolicyPatternOptions;

import static org.junit.jupiter.api.Assertions.assertTrue;

class PolicyTypeProviderTest {

  @Test
  @DisplayName("Standard Target - Account Level (qualLevel 0)")
  void from_StandardAccountLevel() {
    PolicyPattern pattern = new PolicyPattern(Option.none(), Option.none(), Option.none());

    Validation<Seq<ValidationError>, PolicyType> result =
        PolicyTypeProvider.getPolicyType(
            pattern, SnowflakeObjectType.ACCOUNT, new PolicyPatternOptions(false, false));

    assertTrue(result.isValid());
    assertTrue(result.get() instanceof PolicyType.Standard.Global);
  }

  @Test
  @DisplayName("Standard Target - Database Level (qualLevel 1)")
  void from_StandardDatabaseLevel() {
    PolicyPattern pattern = new PolicyPattern(Option.some("MY_DB"), Option.none(), Option.none());

    Validation<Seq<ValidationError>, PolicyType> result =
        PolicyTypeProvider.getPolicyType(
            pattern, SnowflakeObjectType.DATABASE, new PolicyPatternOptions(false, false));

    assertTrue(result.isValid());
    assertTrue(result.get() instanceof PolicyType.Standard.AccountObjectDatabase);
  }

  @Test
  @DisplayName("Standard Target - Schema Level (qualLevel 2)")
  void from_StandardSchemaLevel() {
    PolicyPattern pattern = new PolicyPattern(Option.some("MY_DB"), Option.some("MY_SCHEMA"), Option.none());

    Validation<Seq<ValidationError>, PolicyType> result =
        PolicyTypeProvider.getPolicyType(
            pattern, SnowflakeObjectType.SCHEMA, new PolicyPatternOptions(false, false));

    assertTrue(result.isValid());
    assertTrue(result.get() instanceof PolicyType.Standard.Schema);
  }

  @Test
  @DisplayName("Standard Target - Object Level (qualLevel 3)")
  void from_StandardObjectLevel() {
    PolicyPattern pattern = new PolicyPattern(
            Option.some("MY_DB"), Option.some("MY_SCHEMA"), Option.some("MY_TABLE"));

    Validation<Seq<ValidationError>, PolicyType> result =
        PolicyTypeProvider.getPolicyType(
            pattern, SnowflakeObjectType.TABLE, new PolicyPatternOptions(false, false));

    assertTrue(result.isValid());
    assertTrue(result.get() instanceof PolicyType.Standard.SchemaObject);
  }

  @Test
  @DisplayName("Container Target - Database Level (qualLevel 2 with wildcard schema)")
  void from_ContainerDatabaseLevel() {
    // FUTURE SCHEMAS IN DATABASE MY_DB
    // Missing/wildcard schema implies database level for container
    PolicyPattern pattern = new PolicyPattern(Option.some("MY_DB"), Option.none(), Option.none());

    Validation<Seq<ValidationError>, PolicyType> result =
        PolicyTypeProvider.getPolicyType(
            pattern, SnowflakeObjectType.SCHEMA, new PolicyPatternOptions(true, false));

    assertTrue(result.isValid());
    assertTrue(result.get() instanceof PolicyType.Container.AccountObjectDatabase);
  }

  @Test
  @DisplayName("Container Target - Schema Level (qualLevel 3)")
  void from_ContainerSchemaLevel() {
    // ALL TABLES IN SCHEMA MY_DB.MY_SCHEMA
    PolicyPattern pattern = new PolicyPattern(
            Option.some("MY_DB"), Option.some("MY_SCHEMA"), Option.none());

    Validation<Seq<ValidationError>, PolicyType> result =
        PolicyTypeProvider.getPolicyType(
            pattern, SnowflakeObjectType.TABLE, new PolicyPatternOptions(false, true));

    assertTrue(result.isValid());
    assertTrue(result.get() instanceof PolicyType.Container.Schema);
  }

  @Test
  @DisplayName("Container Target - Database Level (qualLevel 3 with wildcard schema)")
  void from_ContainerDatabaseLevelForAll() {
    // ALL TABLES IN DATABASE MY_DB
    PolicyPattern pattern = new PolicyPattern(Option.some("MY_DB"), Option.none(), Option.none());

    Validation<Seq<ValidationError>, PolicyType> result =
        PolicyTypeProvider.getPolicyType(
            pattern, SnowflakeObjectType.TABLE, new PolicyPatternOptions(false, true));

    assertTrue(result.isValid());
    assertTrue(result.get() instanceof PolicyType.Container.AccountObjectDatabase);
  }

  @Test
  @DisplayName("Container Target - Schema Level (qualLevel 3 for FUTURE_AND_ALL)")
  void from_ContainerSchemaLevelForFutureAndAll() {
    PolicyPattern pattern = new PolicyPattern(
            Option.some("MY_DB"), Option.some("MY_SCHEMA"), Option.none());

    Validation<Seq<ValidationError>, PolicyType> result =
        PolicyTypeProvider.getPolicyType(
            pattern, SnowflakeObjectType.TABLE, new PolicyPatternOptions(false, true));

    assertTrue(result.isValid());
    assertTrue(result.get() instanceof PolicyType.Container.Schema);
  }
}
