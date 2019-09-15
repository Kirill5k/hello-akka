package akka.basic

import akka.actor.{Actor, ActorLogging, ActorSystem, Props}
import com.typesafe.config.ConfigFactory

object Actors9Config extends App {

  class SimpleActor extends Actor with ActorLogging {
    override def receive: Receive = {
      case message => log.info(message.toString)
    }
  }

  val configString =
    """
      |akka {
      | loglevel = DEBUG
      |}
      |""".stripMargin

  val config = ConfigFactory.parseString(configString)
  val system = ActorSystem("configDemo", ConfigFactory.load(config))
  val actor = system.actorOf(Props[SimpleActor], "simpleActor")

  actor ! "sending message"
}
