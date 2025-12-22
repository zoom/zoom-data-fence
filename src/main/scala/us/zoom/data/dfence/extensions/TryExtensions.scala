package us.zoom.data.dfence.extensions

import scala.util.{Failure, Success, Try}

extension [T](tryValue: Try[T])
  def getOrThrow(f: Throwable => T): T =
    tryValue match
      case Success(value) => value
      case Failure(err)   => f(err)
