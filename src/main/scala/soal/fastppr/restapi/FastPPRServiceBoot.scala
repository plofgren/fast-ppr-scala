package soal.fastppr.restapi

import akka.actor.{ActorRef, Props, ActorSystem}
import spray.servlet.WebBoot
import akka.io.{Inet, IO}
import spray.can.Http
import com.typesafe.config.{Config, ConfigFactory}
import com.twitter.cassovary.util.io.AdjacencyListGraphReader
import com.twitter.cassovary.graph.StoredGraphDir._
import com.twitter.cassovary.graph.StoredGraphDir
import com.twitter.cassovary.graph.StoredGraphDir.StoredGraphDir
import soal.fastppr.FastPPRConfiguration
import java.net.InetSocketAddress
import scala.collection.immutable
import spray.can.server.ServerSettings


class FastPPRServiceBoot extends WebBoot {
  // TODO: Read graph location based on config file
  // TODO: Determing the right thread/Actor this code should be running on
  val reader = new AdjacencyListGraphReader("src/test/resources/", "test_graph.txt") {
    override def storedGraphDir: StoredGraphDir = StoredGraphDir.BothInOut
  }
  val graph = reader.toArrayBasedDirectedGraph()

  // TODO: Read from config file
  val fastPPRConfig = FastPPRConfiguration.defaultConfiguration.copy(pprSignificanceThreshold = 4.0f / graph.nodeCount)

  // we need an ActorSystem to host our application in
  val system = ActorSystem("FastPPRService")

  // the service actor replies to incoming HttpRequests
  val serviceActor = system.actorOf(Props[FastPPRServiceActor]{
    new FastPPRServiceActor(graph, fastPPRConfig)
  })

}


object FastPPRServiceBoot extends App {
  val fastPPRServiceBoot = new FastPPRServiceBoot()
  val system = fastPPRServiceBoot.system
  val service = fastPPRServiceBoot.serviceActor
  val conf = ConfigFactory.load()
  val port = conf.getInt("fastppr.restapi.port")

  //IO(Http)(system) ! Http.Bind(service, interface = "localhost", port = port)
  IO(Http)(system) ! Http.Bind(service, new InetSocketAddress(port),100, Nil, None)
}