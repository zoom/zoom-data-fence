package us.zoom.data.dfence.providers.snowflake.models.opaque

import us.zoom.data.dfence.providers.snowflake.grant.builder.SnowflakeObjectType

opaque type ObjectTypeAlias = String

object ObjectTypeAlias:
  def apply(snowflakeObjectType: SnowflakeObjectType): ObjectTypeAlias =
    snowflakeObjectType.getAliasFor()
