package org.codingteam.icfpc2017

import java.io.{File, PrintStream}
import java.time.Instant

import org.codingteam.icfpc2017.Common.Punter
import org.codingteam.icfpc2017.onlinegamer.OneBotOnServerGamer
import org.codingteam.icfpc2017.strategy.{FutureStrategy, _}

object AppEntry extends App with Logging {

  private def run(): Unit = {
    Logging.outputStream = Some(System.out)
    args match {
      case Array("--test-map", mapFilePath) =>
        val m = GameMap.Map.fromJsonFile(mapFilePath)
        val map = GraphMap.fromMap(m)
        log.debug(map)
        log.debug(map.getMineNodes)
      // log.debug(map.toGraph())

      case Array("--test-move-parse") =>
        //val moveStr = """{"claim":{"punter":0,"source":0,"target":1}}"""
        val moveStr = """{"pass":{"punter":1}}"""
        val move = Messages.parseMoveStr(moveStr)
        log.debug(move)

      case Array("--test-parse", path) =>
        val message = Messages.parseServerMessageFile(path)
        log.debug(message)

      case Array("--duck", path) =>
        val m = GameMap.Map.fromJsonFile(path)
        val map = GraphMap.fromMap(m)
        val n1 = map.getMineNodes.head
        val n2 = n1.neighbors.head

        map.mark(n1.value, n2.value, Punter(666))
        log.debug(map)
        log.debug(map.getFreeEdges())
        log.debug(map.getPunterSubgraph(Punter(666)))
        log.debug(map.score(Punter(666), None))

      case Array("--tcp", host, Parsing.I(port)) =>
        runTcpLoop(host, port, None, "codingpunter")

      case Array("--online-gamer", name, maps) =>
        val mapz = maps.split(',').toList
        new OneBotOnServerGamer().run(mapz, name)

      case Array("--tcp-with-strategy", host, Parsing.I(port), strategy) =>
        runTcpLoop(host, port, None, strategy)

      case Array("--tcp-with-log", host, Parsing.I(port), name) =>
        runTcpLoop(host, port, Some(s"logs/game-${Instant.now().toEpochMilli}.lson"), name)

      case Array("--offline") =>
        Logging.outputStream = None
        runOfflineLoop(None, "delegating")

      case Array("--offline-with-log", name) =>
        Logging.outputStream = Some(new PrintStream(new File(s"logs/game-${Instant.now().toEpochMilli}.lson")))
        runOfflineLoop(Some(s"logs/game-${Instant.now().toEpochMilli}.lson"), name)

      case Array("--mixed-coefficients", Parsing.I(port), Parsing.D(gw), Parsing.D(fw), Parsing.D(mw), Parsing.D(cw), Parsing.D(dw), Parsing.D(rw), Parsing.D(a)) =>
        val strategy = new MixedStrategy(Seq(
          (gw, new GreedyStrategy()),
          (fw, new FutureStrategy()),
          (mw, new MineOccupationStrategy()),
          (cw, new ComponentConnectorStrategy()),
          (dw, new DumbObstructorStrategy()),
          (rw, new RandomConnectorStrategy())),
          useBackgroundThreads = true,
          alpha = a)
        HandlerLoop.runLoop(StreamParser.connect("127.0.0.1", port, None), strategy, "whatever")

      case _ =>
        log.info("Hello")
        log.info("Use --offline")
    }

  }

  def runTcpLoop(host: String, port: Int, log: Option[String], name: String): Unit = {
    val strategy = selectStrategy(name)
    HandlerLoop.runLoop(StreamParser.connect(host, port, log), strategy, name)
  }

  def runOfflineLoop(log: Option[String], name: String): Unit = {
    val strategy = selectStrategy(name)
    HandlerLoop.runOfflineMove(StreamParser.connectToStdInOut(log), strategy, "codingpunter")
  }

  def selectStrategy(name: String): Strategy = {
    name match {
      case "codingpunter-dumb-obstructor" => new DelegatingStrategy(Seq(new DumbObstructorStrategy()), true)
      case "random-codingpunter" => new DelegatingStrategy(Seq(new RandomConnectorStrategy()), true)
      case "codingpunter" => new DelegatingStrategy(Seq(new GreedyStrategy()), true)
      case "connector" => new DelegatingStrategy(Seq(new ComponentConnectorStrategy()), true)
      case "mixed" => new MixedStrategy(Seq(
        (2.0, new GreedyStrategy()),
        (1.9, new FutureStrategy()),
        (1.0, new MineOccupationStrategy()),
        (1.0, new ComponentConnectorStrategy()),
        (1.0, new DumbObstructorStrategy()),
        (0.5, new RandomConnectorStrategy())),
        useBackgroundThreads = true,
        alpha = 1.5)
      case "delegating" => new DelegatingStrategy(Seq(
        new GreedyStrategy(),
        new FutureStrategy(),
        new MineOccupationStrategy(),
        new ComponentConnectorStrategy(),
        new DumbObstructorStrategy(),
        new RandomConnectorStrategy()),
        useBackgroundThreads = true)
      case "antihero" => new AntiheroStrategy()
      case _ => throw new Exception("unsupported name: " + name)
    }
  }

  run()
}
