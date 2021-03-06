package org.codingteam.icfpc2017

import org.codingteam.icfpc2017.Common.Punter
import org.json4s._
import org.json4s.jackson.JsonMethods._
import org.json4s.jackson.Serialization

import scala.io.Source
import scalax.collection.edge.LBase.LEdgeImplicits
import scalax.collection.edge.LUnDiEdge
import scalax.collection.mutable.Graph

object GameMap {

  type SiteId = BigInt

  abstract sealed class Node {
    def id: SiteId
  }

  case class Site(override val id: SiteId) extends Node

  case class Mine(override val id: SiteId) extends Node

  object SiteImplicit extends LEdgeImplicits[Punter]

  case class River(source: SiteId, target: SiteId) {
    def toEdge(map: Map): LUnDiEdge[Node] = {
      LUnDiEdge(map.siteMap(source), map.siteMap(target))(None)
    }
  }

  implicit val formats = Serialization.formats(NoTypeHints)

  object Map {
    def fromJson(str: String): Map = {
      return parse(str).extract[Map]
    }

    def fromJsonFile(path: String): Map = {
      val source = Source.fromFile(path)
      return parse(source.reader()).extract[Map]
    }

    def createEmpty = new Map(IndexedSeq(), IndexedSeq(), IndexedSeq())
  }

  class Map(val sites: IndexedSeq[Site],
            val rivers: IndexedSeq[River],
            val mines: IndexedSeq[SiteId]) {

    var siteMap = sites.map(site => (site.id, siteToNode(site))).toMap

    def siteToNode(site: Site): Node = {
      val isMine = mines.contains(site.id)
      //println(s"siteToNode: $site ${this.mines} ${isMine}")
      if (isMine) {
        Mine(site.id)
      } else {
        site
      }
    }

    def toJson(): String = {
      return Serialization.write(this)
    }

    def toGraph(): Graph[Node, LUnDiEdge] = {
      val edges = rivers.map(r => r.toEdge(this))
      return Graph.from(siteMap.values, edges)
    }

    override def toString(): String = {
      toJson()
    }
  }

}
