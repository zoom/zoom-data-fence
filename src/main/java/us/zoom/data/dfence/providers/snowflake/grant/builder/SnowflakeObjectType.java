package us.zoom.data.dfence.providers.snowflake.grant.builder;

import lombok.Getter;

public enum SnowflakeObjectType {
  ACCOUNT(0, null),
  ALERT(3, null),
  APPLICATION_ROLE(1, null),
  CLASS(1, null),
  DATABASE(1, null),
  DATABASE_ROLE(2, null),
  DIRECTORY_TABLE(3, null),
  EVENT_TABLE(3, null),
  EXTERNAL_TABLE(3, "TABLE"),
  FILE_FORMAT(3, null),
  FUNCTION(3, null),
  ICEBERG_TABLE(3, "TABLE"),
  INTEGRATION(1, null),
  INSTANCE(3, null),
  MASKING_POLICY(3, null),
  MATERIALIZED_VIEW(3, "VIEW"),
  NETWORK_POLICY(1, null),
  NETWORK_RULE(1, null),
  NOTEBOOK(3, null),
  PASSWORD_POLICY(3, null),
  PIPE(3, null),
  PROCEDURE(3, null),
  RESOURCE_MONITOR(1, null),
  ROLE(1, null),
  ROW_ACCESS_POLICY(3, null),
  SCHEMA(2, null),
  SECRET(3, null),
  SEQUENCE(3, null),
  SESSION_POLICY(3, null),
  SEMANTIC_VIEW(3, null),
  STAGE(3, null),
  STREAM(3, null),
  STREAMLIT(3, null),
  TABLE(3, null),
  TAG(3, null),
  TASK(3, null),
  USER(1, null),
  VIEW(3, null),
  VOLUME(1, null),
  WAREHOUSE(1, null),
  COMPUTE_POOL(1, null),
  IMAGE_REPOSITORY(3, null);

  @Getter private final Integer qualLevel;

  @Getter private final String objectType;

  @Getter private final String objectTypePlural;

  private final String aliasFor;

  SnowflakeObjectType(Integer qualLevel, String aliasFor) {
    this.qualLevel = qualLevel;
    this.aliasFor = aliasFor;
    this.objectType = this.name().replace("_", " ");
    // Hooked on phonics works for me.
    if (this.objectType.endsWith("Y")) {
      this.objectTypePlural = this.objectType.substring(0, this.objectType.length() - 1) + "IES";
    } else {
      this.objectTypePlural = this.objectType + "S";
    }
  }

  public String getAliasFor() {
    if (this.aliasFor == null) {
      return this.name();
    }
    return this.aliasFor;
  }
}
