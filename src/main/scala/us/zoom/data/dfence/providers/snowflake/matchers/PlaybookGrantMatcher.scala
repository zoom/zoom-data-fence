package us.zoom.data.dfence.providers.snowflake.matchers

import org.slf4j.LoggerFactory
import us.zoom.data.dfence.providers.snowflake.models.{PlaybookGrant, SnowflakeGrant}

trait PlaybookGrantMatcher:
  def matchesPlaybookGrant(playbookGrant: PlaybookGrant): Boolean

object PlaybookGrantMatcher:
  def apply(snowflakeGrant: SnowflakeGrant): PlaybookGrantMatcher =
    PlaybookGrantMatcherLive(snowflakeGrant)

private class PlaybookGrantMatcherLive(snowflakeGrant: SnowflakeGrant) extends PlaybookGrantMatcher:

  private val log = LoggerFactory.getLogger(getClass)

  def matchesPlaybookGrant(playbookGrant: PlaybookGrant): Boolean =
    val matchers = SnowflakeGrantMatchers(playbookGrant)
    import matchers.*

    val result = snowflakeGrant match
      case SnowflakeGrant(GrantObjectType(), GrantPrivilege(), GrantType(), GrantObjectName()) => true
      case _                                                                                   => false

    if !result then
      log.info(
        "Playbook grant {} match failed for snowflake grant {}",
        playbookGrant,
        snowflakeGrant
      )
    else
      log.info(
        "Playbook grant {} matched snowflake grant {}",
        playbookGrant,
        snowflakeGrant
      )
    result
