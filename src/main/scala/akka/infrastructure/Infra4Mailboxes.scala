package akka.infrastructure

import akka.actor.{Actor, ActorLogging, ActorSystem, Props}
import akka.dispatch.{ControlMessage, PriorityGenerator, UnboundedPriorityMailbox}
import com.typesafe.config.{Config, ConfigFactory}

object Infra4Mailboxes extends App {
  val system = ActorSystem("Infra4Mailboxes", ConfigFactory.load().getConfig("mailboxesDemo"))

  class SimpleActor extends Actor with ActorLogging {
    override def receive: Receive = {
      case message => log.info(s"$message")
    }
  }

  class SupportTickerPriorityMailbox(settings: ActorSystem.Settings, config: Config)
    extends UnboundedPriorityMailbox(PriorityGenerator {
      case message: String if message.startsWith("[P0]") => 0
      case message: String if message.startsWith("[P1]") => 1
      case message: String if message.startsWith("[P2]") => 2
      case message: String if message.startsWith("[P3]") => 3
      case _ => 4
    })

  val logger = system.actorOf(Props[SimpleActor].withDispatcher("support-ticket-dispatcher"))
//  logger ! "[P3] message"
//  logger ! "[P0] message"
//  logger ! "[P2] message"
//  logger ! "[P1] message"

  /**
   * Control aware mailbox
    */

  case object ManagementTicket extends ControlMessage

  val controlAwareActor = system.actorOf(Props[SimpleActor].withMailbox("control-mailbox"))
  controlAwareActor ! "[P3] message"
  controlAwareActor ! "[P0] message"
  controlAwareActor ! "[P2] message"
  controlAwareActor ! ManagementTicket
}
