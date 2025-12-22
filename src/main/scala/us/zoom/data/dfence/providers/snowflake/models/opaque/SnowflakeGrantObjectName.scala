package us.zoom.data.dfence.providers.snowflake.models.opaque

opaque type SnowflakeGrantObjectName = String

object SnowflakeGrantObjectName:
  def apply(name: String): SnowflakeGrantObjectName                       = name.trim
  extension (grantObjectName: SnowflakeGrantObjectName) def toStr: String = grantObjectName.trim
