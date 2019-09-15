package akka.basic

import akka.actor.{Actor, ActorLogging, ActorRef, ActorSystem, PoisonPill, Props}
import akka.basic.Actors11Lifecycle.LifecycleActor.StartChild
import akka.basic.Actors11Lifecycle.ParentActor.FailChild

object Actors11Lifecycle extends App {
  val system = ActorSystem("system")

  object LifecycleActor {
    case object StartChild
  }
  class LifecycleActor extends Actor with ActorLogging {
    override def receive: Receive = {
      case StartChild => context.actorOf(Props[LifecycleActor], "child")
    }

    override def preStart(): Unit = log.info("starting up")
    override def postStop(): Unit = log.info("stopping down")
  }

  object ChildActor {
    case object Fail
  }
  class ChildActor extends Actor with ActorLogging {
    import ChildActor._
    override def receive: Receive = {
      case Fail =>
        log.warning("child actor has failed")
        throw new RuntimeException("failure")
    }

    override def preStart(): Unit = log.info("child about to start")

    override def postStop(): Unit = log.info("child has stopped")

    override def preRestart(reason: Throwable, message: Option[Any]): Unit =
      log.info(s"child actor restarting because of ${reason.getMessage}")

    override def postRestart(reason: Throwable): Unit =
      log.info(s"child actor has restarted. reason - ${reason.getMessage}")
  }

  object ParentActor {
    case object FailChild
  }
  class ParentActor extends Actor with ActorLogging {
    import ChildActor._
    import ParentActor._
    override def receive: Receive = withChild(context.actorOf(Props[ChildActor], "childactor"))

    def withChild(child: ActorRef): Receive = {
      case FailChild => child ! Fail
    }
  }

  val parent = system.actorOf(Props[LifecycleActor], "parent")
  parent ! StartChild
  parent ! PoisonPill
  Thread.sleep(1000)
  val anotherParent = system.actorOf(Props[ParentActor], "anotherparent")
  anotherParent ! FailChild
}
