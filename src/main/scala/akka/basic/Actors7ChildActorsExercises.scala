package akka.basic

import akka.actor.ActorSystem
import akka.actors.DistributedWordCounter

object Actors7ChildActorsExercises extends App {
  val system = ActorSystem("system")

  val master = system.actorOf(DistributedWordCounter.props, "distributed-word-counter")

  master ! DistributedWordCounter.Initialise(5)
  master ! DistributedWordCounter.WordCountRequest("scala is decent")
  master ! DistributedWordCounter.WordCountRequest("akka is kind of decent as well")
  master ! DistributedWordCounter.WordCountRequest("another message")
  master ! DistributedWordCounter.WordCountEnquiryRequest(1)
  master ! DistributedWordCounter.WordCountEnquiryRequest(2)
  master ! DistributedWordCounter.WordCountEnquiryRequest(10)
}
