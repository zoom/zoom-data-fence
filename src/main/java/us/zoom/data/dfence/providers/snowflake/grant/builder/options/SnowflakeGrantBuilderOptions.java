package us.zoom.data.dfence.providers.snowflake.grant.builder.options;

import lombok.Data;

@Data
public class SnowflakeGrantBuilderOptions {
  UnsupportedRevokeBehavior unsupportedRevokeBehavior = UnsupportedRevokeBehavior.IGNORE;
  Boolean suppressErrors = false;
}
