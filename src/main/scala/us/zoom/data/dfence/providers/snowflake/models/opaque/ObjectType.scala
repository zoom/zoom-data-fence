package us.zoom.data.dfence.providers.snowflake.models.opaque

import us.zoom.data.dfence.providers.snowflake.grant.builder.SnowflakeObjectType

opaque type ObjectType = String

object ObjectType:
  def apply(snowflakeObjectType: SnowflakeObjectType): ObjectType =
    snowflakeObjectType.getObjectType()
