package akka.basic

import akka.actor.{Actor, ActorLogging, ActorRef, ActorSystem, Props, Terminated}

object Actors10StartingAndStopping extends App {

  val system = ActorSystem("StoppingActorsDemo")

  object Parent {
    case class StartChild(name: String)
    case class StopChild(name: String)
    case object StopViaContext
  }
  class Parent extends Actor with ActorLogging {
    import Parent._
    override def receive: Receive = withChildren(Map())

    def withChildren(children: Map[String, ActorRef]): Receive = {
      case StartChild(name) =>
        log.info("creating child {}", name)
        context.become(withChildren(children + (name -> context.actorOf(Props[Child], name))))
      case StopChild(name) if children.contains(name) =>
        log.info("stopping child {}", name)
        context.stop(children(name))
      case StopViaContext =>
        log.info("stopping myself")
        context.stop(self)
      case message => log.info(message.toString)
    }
  }

  class Child extends Actor with ActorLogging {
    override def receive: Receive = {
      case message => log.info(message.toString)
    }

    override def postStop(): Unit = {
      log.info("I am dead now")
    }
  }

  class Watcher extends Actor with ActorLogging {
    import Parent._

    override def receive: Receive = {
      case StartChild(name) =>
        val child = context.actorOf(Props[Child], name)
        log.info(s"started and watching child $name")
        context.watch(child)
      case Terminated(ref) =>
        log.info(s"the child actor that I am watching $ref has been stopped")
    }
  }

  val parent = system.actorOf(Props[Parent], "parent")
  val watcher = system.actorOf(Props[Watcher], "watcher")
  parent ! StartChild("child1")
  Thread.sleep(250)
  val child = system.actorSelection("/user/parent/child1")
  child ! "hi kid"
  parent ! StopChild("child1")

  val looseActor = system.actorOf(Props[Child], "looseActor")
  looseActor ! "hello, loose actor"
  looseActor ! PoisonPill

  watcher ! StartChild("child3")
  val watchedChild = system.actorSelection("/user/watcher/child3")
  Thread.sleep(250)
  watchedChild ! PoisonPill
}
