package soal.fastppr.restapi

import akka.actor.{Props, ActorSystem}
import spray.servlet.WebBoot
import akka.io.IO
import spray.can.Http

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
  implicit val system = ActorSystem("FastPPRService")
  val log = system.log
  val service = system.actorOf(Props[FastPPRServiceActor]{
    new FastPPRServiceActor("Pokec!")
  })

  IO(Http) ! Http.Bind(service, interface = "localhost", port = 8081)
}