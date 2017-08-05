package org.codingteam.icfpc2017

import org.codingteam.icfpc2017.GameMap.{Mine, Site}
import java.time.{Clock, Instant}

import org.codingteam.icfpc2017.Common.Punter

object AppEntry extends App {

  private def run(): Unit = {
    args match {
      case Array("--test-map", mapFilePath) =>
        val m = GameMap.Map.fromJsonFile(mapFilePath)
        val map = GraphMap.fromMap(m)
        println(map)
        println(map.getMineNodes)
        // println(map.toGraph())

      case Array("--test-move-parse") =>
        //val moveStr = """{"claim":{"punter":0,"source":0,"target":1}}"""
        val moveStr = """{"pass":{"punter":1}}"""
        val move = Messages.parseMoveStr(moveStr)
        println(move)

      case Array("--test-parse", path) =>
        val message = Messages.parseServerMessageFile(path)
        println(message)

      case Array("--duck", path) =>
        val m = GameMap.Map.fromJsonFile(path)
        val map = GraphMap.fromMap(m)
        val n1 = map.getMineNodes.head
        val n2 = n1.neighbors.head

        map.mark(n1.value, n2.value, Punter(666))
        println(map)
        println(map.getFreeEdges())
        println(map.getPunterSubgraph(Punter(666)))
        println(map.score(Punter(666)))

      case Array("--tcp", host, Parsing.I(port)) =>
        runTcpLoop(host, port, None)

      case Array("--tcp-with-log", host, Parsing.I(port)) =>
        runTcpLoop(host, port, Some(s"logs/game-${Instant.now().toEpochMilli}.lson"))
      case _ =>
        println("Hello!")
    }

  }

  def runTcpLoop(host: String, port: Int, log: Option[String]): Unit = {
    HandlerLoop.runLoop(TcpInterface.connect(host, port, log), strategy, offline = false)
  }

  // TODO: implement real strategy.

  lazy val strategy = new DelegatingStrategy(Seq(new GreedyStrategy()))

  run()
}
