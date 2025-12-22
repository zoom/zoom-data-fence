package us.zoom.data.dfence.providers.snowflake.models

import us.zoom.data.dfence.providers.snowflake.models.opaque.{GrantPrivilege, ObjectType, ObjectTypeAlias}

import scala.collection.parallel.{ParMap, ParSet}

case class PrivilegeGrantIndex(
  privilegeIndex: ParMap[GrantPrivilege, ParSet[PlaybookGrant]],
  objectTypeIndex: ParMap[ObjectType, ParSet[PlaybookGrant]],
  objectAliasIndex: ParMap[ObjectTypeAlias, ParSet[PlaybookGrant]])
