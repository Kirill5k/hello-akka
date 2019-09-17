package akka.infrastructure

import akka.actor.{Actor, ActorLogging, ActorSystem, Props, Stash}

object Infra5Stashes extends App {
  val system = ActorSystem("system")

  case object ResourceActor {
    case object Open
    case object Close
    case object Read
    case class Write(data: String)
  }

  class ResourceActor extends Actor with ActorLogging with Stash {
    import ResourceActor._
    private var innerData: String = ""

    override def receive: Receive = closed

    def closed: Receive = {
      case Open =>
        log.info("opening the resource")
        unstashAll()
        context.become(opened)
      case message =>
        log.info(s"[closed] stashing a message $message")
        stash()
    }

    def opened: Receive = {
      case Read =>
        log.info(s"[opened] reading a resource: $innerData")
      case Write(data) =>
        log.info(s"[opened] writing data $data")
      case Close =>
        log.info("closing the resource")
        unstashAll()
        context.become(closed)
      case message =>
        log.info(s"[opened] stashing unexpected message $message")
        stash()
    }
  }

  import ResourceActor._
  val resourceActor = system.actorOf(Props[ResourceActor], "resource-actor")

  resourceActor ! Read
  resourceActor ! Open
  resourceActor ! Open
  resourceActor ! Write("new inner data")
  resourceActor ! Close
  resourceActor ! Read

}
