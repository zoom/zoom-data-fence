package us.zoom.data.dfence.providers.snowflake.models.opaque

opaque type GrantPrivilege = String

object GrantPrivilege:
  def apply(privilege: String): GrantPrivilege = privilege.trim.toUpperCase()
