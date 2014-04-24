package soal.fastppr.restapi

import akka.actor.Actor
import spray.routing._
import spray.http._
import MediaTypes._

// we don't implement our route structure directly in the service actor because
// we want to be able to test it independently, without having to spin up an actor
class FastPPRServiceActor(val graphName: String) extends Actor with FastPPRService {

  // the HttpService trait defines only one abstract member, which
  // connects the services environment to the enclosing actor or test
  def actorRefFactory = context

  // this actor only runs our route, but you could add
  // other things here, like request stream processing
  // or timeout handling
  def receive = runRoute(myRoute)
}


// this trait defines our service behavior independently from the service actor
trait FastPPRService extends HttpService {
  def graphName: String

  val myRoute =
    path("") {
       parameters('startId.as[Int], 'targetId.as[Int]) {
         (startId: Int, targetId: Int) => {
           val sum = startId + targetId
           respondWithMediaType(`application/json`) {
              complete("{ \"pprEstimate\": " + sum + "\n\"graphName\":\"" + graphName + "\"}")
           }
         }
       }
    }
}