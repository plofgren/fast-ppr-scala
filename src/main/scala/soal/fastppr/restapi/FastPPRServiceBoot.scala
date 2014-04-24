package soal.fastppr.restapi

import akka.actor.{Props, ActorSystem}
import spray.servlet.WebBoot
import akka.io.IO
import spray.can.Http
import com.typesafe.config.{Config, ConfigFactory}

// this class is instantiated by the servlet initializer
// it needs to have a default constructor and implement
// the spray.servlet.WebBoot trait
class FastPPRServiceBoot extends WebBoot {

  // we need an ActorSystem to host our application in
  val system = ActorSystem("FastPPRService")

  // the service actor replies to incoming HttpRequests
  val serviceActor = system.actorOf(Props[FastPPRServiceActor]{
    new FastPPRServiceActor("Pokec!")
  })

}

object FastPPRServiceBoot extends App {
  val fastPPRServiceBoot = new FastPPRServiceBoot()
  val system = fastPPRServiceBoot.system
  val service = fastPPRServiceBoot.serviceActor
  val conf = ConfigFactory.load()
  val port = conf.getInt("fastppr.restapi.port")

  IO(Http)(system) ! Http.Bind(service, interface = "localhost", port = port)
}