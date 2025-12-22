package us.zoom.data.dfence.providers.snowflake.models

import us.zoom.data.dfence.providers.snowflake.grant.builder.SnowflakeObjectType
import us.zoom.data.dfence.providers.snowflake.models.opaque.{GrantPrivilege, SnowflakeGrantObjectName}
import us.zoom.data.dfence.providers.snowflake.models.SnowflakeGrantModel

import scala.util.Try

// Typesafe version of SnowflakeGrantModel
case class SnowflakeGrant(
  snowflakeObjectType: SnowflakeObjectType,
  privilege: GrantPrivilege,
  grantType: SnowflakeGrantType,
  name: SnowflakeGrantObjectName)

object SnowflakeGrant:
  def from(model: SnowflakeGrantModel): Try[SnowflakeGrant] = Try {
    SnowflakeGrant(
      snowflakeObjectType = SnowflakeObjectType.valueOf(model.grantedOn()),
      privilege = GrantPrivilege(model.privilege()),
      grantType = (model.future(), model.all()) match
        case (true, false) => SnowflakeGrantType.Future
        case (false, true) => SnowflakeGrantType.All
        case (_, _)        => SnowflakeGrantType.Standard
      ,
      name = SnowflakeGrantObjectName(model.name())
    )
  }
