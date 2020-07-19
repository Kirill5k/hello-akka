package akka.basic

import akka.actor.{Actor, ActorRef, ActorSystem, Props}

object Actors6ChildActor extends App {
  val system = ActorSystem("childActorSystem")

  object Parent {
    final case class CreateChild(name: String)
    final case class TellChild(message: String)
  }

  class Parent extends Actor {
    import Parent._

    override def receive: Receive = withChildren(List())

    def withChildren(children: List[ActorRef]): Receive = {
      case CreateChild(name) =>
        println(s"${self.path} creating child $name")
        context.become(withChildren(context.actorOf(Props[Child], name) :: children))
      case TellChild(message) => children.foreach(_ forward message)
    }
  }

  class Child extends Actor {
    override def receive: Receive = {
      case message => println(s"${self.path} I got: $message")
    }
  }

  val parent = system.actorOf(Props[Parent], "parent")
  parent ! Parent.CreateChild("child1")
  parent ! Parent.CreateChild("child2")
  parent ! Parent.TellChild("message for my children")

  val childSelection = system.actorSelection("/user/parent/child1")
  childSelection ! "I found you"
}
