package us.zoom.data.dfence.extensions

import scala.collection.mutable

extension [K, V](map1: mutable.HashMap[K, mutable.HashSet[V]])
  def mergeHashMaps(
    map2: mutable.HashMap[K, mutable.HashSet[V]]
  ): mutable.HashMap[K, mutable.HashSet[V]] =
    if map1.size < map2.size then map1.mergeTo(map2)
    else map2.mergeTo(map1)

extension [K, V](sourceMap: mutable.HashMap[K, mutable.HashSet[V]])
  def mergeTo(
    destMap: mutable.HashMap[K, mutable.HashSet[V]]
  ): mutable.HashMap[K, mutable.HashSet[V]] =
    sourceMap.foreach { case (k, v) =>
      destMap.getOrElseUpdate(k, mutable.HashSet()) ++= v
    }
    destMap
