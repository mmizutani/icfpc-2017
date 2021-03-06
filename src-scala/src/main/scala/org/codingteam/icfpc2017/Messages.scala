package org.codingteam.icfpc2017

import org.codingteam.icfpc2017.Common._
import org.codingteam.icfpc2017.GameMap._
import org.codingteam.icfpc2017.futures.Future
import org.json4s.JsonDSL._
import org.json4s._
import org.json4s.jackson.JsonMethods._
import org.json4s.jackson.Serialization

import scala.io.Source

object Messages {

  implicit val formats = Serialization.formats(NoTypeHints)


  abstract sealed class Message

  trait Serializable {
    def toJson(): JObject
  }

  case class HelloRq(punter: String) extends Message with Serializable {
    def toJson(): JObject = {
      "me" -> punter
    }
  }

  case class HelloRs(punter: String) extends Message

  object HelloRs {
    def unapply(json: JValue): Option[HelloRs] = {
      for {
        JString(name) <- (json \ "you").toOption
      } yield HelloRs(name)
    }
  }

  case class Settings(futures: Boolean, options: Boolean)

  case class SetupRq(punter: BigInt, punters: Int, map: Map, settings: Option[Settings]) extends Message

  object SetupRq {
    def unapply(json: JValue): Option[SetupRq] = {
      hasKey(json, "punter") toOption json.extract[SetupRq]
    }
  }

  case class SetupRs(punter: Punter, futures: Option[List[Future]], state: JValue = JNothing) extends Message with Serializable {
    def futuresJson(): JValue = {
      futures match {
        case None => JNothing
        case Some(list) => list.map(_.toJson)
      }
    }

    def toJson(): JObject = {
      if (state == JNothing)
        JObject("ready" -> punter.id, "futures" -> futuresJson)
      else
        JObject("ready" -> punter.id, "state" -> state, "futures" -> futuresJson)
    }
  }

  abstract sealed class Move extends Message with Serializable {
    var state: JValue = JNothing

    def punter: Punter
  }

  object Move {
    def unapply(json: JValue): Option[Move] = {
      parseMove(json)
    }
  }

  case class MoveRq(moves: List[Move], state: JValue = JNothing) extends Message

  object MoveRq {
    def getMovesList(json: JValue): List[Move] = {
      (json \ "move" \ "moves").toOption match {
        case None => List()
        case Some(JArray(moves)) =>
          //println(moves)
          for {move <- moves} yield parseMove(move).get
        case _ => List()
      }
    }

    def unapply(json: JValue): Option[MoveRq] = {
      hasKey(json, "move") toOption {
        val moves = getMovesList(json)
        val state = json \ "state"
        MoveRq(moves, state)
      }
    }
  }

  case class Claim(override val punter: Punter, source: Site, target: Site) extends Move {
    def toJson(): JObject = if (state == JNothing) {
      "claim" ->
        ("punter" -> punter.id) ~
          ("source" -> source.id) ~
          ("target" -> target.id)
    } else {
      JObject(
        "claim" ->
          (("punter" -> punter.id) ~
            ("source" -> source.id) ~
            ("target" -> target.id)),
        "state" -> state
      )
    }
  }

  object IsClaim {
    def unapply(json: JValue): Option[Claim] = {
      for {
        claim <- (json \ "claim").toOption
        JInt(id) <- (claim \ "punter").toOption
        JInt(source) <- (claim \ "source").toOption
        JInt(target) <- (claim \ "target").toOption
      } yield Claim(Punter(id), Site(source), Site(target))
    }
  }

  case class AnOption(override val punter: Punter, source: Site, target: Site) extends Move {
    def toJson(): JObject = if (state == JNothing) {
      "option" ->
        ("punter" -> punter.id) ~
          ("source" -> source.id) ~
          ("target" -> target.id)
    } else {
      JObject(
        "option" ->
          (("punter" -> punter.id) ~
            ("source" -> source.id) ~
            ("target" -> target.id)),
        "state" -> state
      )
    }
  }

  object IsOption {
    def unapply(json: JValue): Option[AnOption] = {
      for {
        claim <- (json \ "option").toOption
        JInt(id) <- (claim \ "punter").toOption
        JInt(source) <- (claim \ "source").toOption
        JInt(target) <- (claim \ "target").toOption
      } yield AnOption(Punter(id), Site(source), Site(target))
    }
  }
  case class Splurge(override val punter: Punter, route: List[SiteId]) extends Move {
    def toJson(): JObject = {
      JObject(
        "splurge" ->
          (("punter" -> punter.id) ~
            ("route" -> route)),
        "state" -> state
      )
    }
  }

  object IsSplurge {
    def unapply(json: JValue): Option[Splurge] = {
      for {
        splurge <- (json \ "splurge").toOption
        JInt(id) <- (splurge \ "punter").toOption
        JArray(arr) <- (splurge \ "route").toOption
      } yield Splurge(Punter(1),
        arr flatMap {
          case JInt(site) => Some(site)
          case _ => None
        }
      )
    }
  }

  case class Pass(override val punter: Punter) extends Move {
    def toJson(): JObject = if (state == JNothing) {
      "pass" -> ("punter" -> punter.id)
    } else {
      ("pass" -> ("punter" -> punter.id)) ~ ("state" -> state)
    }
  }

  object Pass {
    def unapply(json: JValue): Option[Pass] = {
      for {
        JInt(id) <- (json \ "pass" \ "punter").toOption
      } yield Pass(Punter(id))
    }
  }

  case class Score(punter: Punter, score: BigInt) extends Message

  object Score {
    def unapply(json: JValue): Option[Score] = {
      for {
        JInt(punter) <- (json \ "punter").toOption
        JInt(score) <- (json \ "score").toOption
      } yield Score(Punter(punter), score)
    }
  }

  case class Stop(moves: List[Move], scores: List[Score], state: JValue = JNothing) extends Message {
    def getScore(punter: Punter): BigInt = {
      scores find (_.punter == punter) map (_.score) getOrElse 0
    }
  }

  object Stop {
    def getScoresList(json: JValue): List[Score] = {
      (json \ "stop" \ "scores").toOption match {
        case None => List()
        case Some(JArray(scores)) =>
          //println(scores)
          scores.map {
            case Score(score) => score
          }
        case _ => List()
      }
    }

    def unapply(json: JValue): Option[Stop] = {
      hasKey(json, "stop") toOption {
        // FIXME
        val state = json \ "state"
        Stop(List(), getScoresList(json), state)
      }
    }
  }

  case class Timeout(timeout: Double) extends Message

  object IsTimeout {
    def unapply(json: JValue): Option[Timeout] = {
      hasKey(json, "timeout") toOption {
        json.extract[Timeout]
      }
    }
  }

  def hasKey(json: JValue, key: String): Boolean = {
    val v = json \ key
    // TODO: JNull check ?
    v != JNothing
  }

  def parseMove(json: JValue): Option[Move] = {
    json match {
      case Pass(pass) => Some(pass)
      case IsClaim(claim) => Some(claim)
      case IsSplurge(splurge) => Some(splurge)
      case IsOption(option) => Some(option)
      case _ => None // throw new Exception(json.toString)
    }
  }

  def parseMoveStr(str: String): Option[Move] = {
    parseMove(parse(str))
  }

  def parseServerMessageJson(json: JValue): Option[Message] = {
    json match {
      case HelloRs(hello) => Some(hello)
      case SetupRq(setup) => Some(setup)
      case MoveRq(move) => Some(move)
      case Stop(stop) => Some(stop)
      case IsTimeout(timeout) => Some(timeout)
      case _ => None
    }
  }

  def parseServerMessageStr(str: String): Option[Message] = {
    parseServerMessageJson(parse(str))
  }

  def parseServerMessageFile(path: String): Option[Message] = {
    val source = Source.fromFile(path)
    parseServerMessageJson(parse(source.reader()))
  }
}
