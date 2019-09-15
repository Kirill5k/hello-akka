package akka.basic

import akka.actor.{Actor, ActorLogging, ActorSystem, Props}
import akka.event.Logging

object Actors8Logging extends App {

  val system = ActorSystem("system")

  class ActorWithExplicitLogger extends Actor {
    val logger = Logging(context.system, this)

    override def receive: Receive = {
      case message => logger.info(message.toString)
    }
  }

  class ActorWithActorLogger extends Actor with ActorLogging {
    override def receive: Receive = {
      case message => log.info(message.toString)
    }
  }

  val explicitLogger = system.actorOf(Props[ActorWithExplicitLogger], "explicitLogger")
  val actorLogger = system.actorOf(Props[ActorWithActorLogger], "actorLogger")

  explicitLogger ! "helolo"
  actorLogger ! "ololeh"
}
