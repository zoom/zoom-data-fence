package us.zoom.data.dfence.providers.snowflake.models

import us.zoom.data.dfence.providers.snowflake.grant.builder.SnowflakeObjectType
import us.zoom.data.dfence.providers.snowflake.models.opaque.GrantPrivilege

// Typesafe version of PlaybookPrivilegeGrant
case class PlaybookGrant(
  objectType: SnowflakeObjectType,
  pattern: PlaybookPattern,
  privileges: List[GrantPrivilege],
  grantType: PlaybookGrantType,
  enable: Boolean
)
