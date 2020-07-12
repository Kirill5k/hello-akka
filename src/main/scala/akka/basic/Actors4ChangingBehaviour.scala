package akka.basic

import akka.actor.{Actor, ActorRef, ActorSystem, Props}

object Actors4ChangingBehaviour extends App {

  class ChildActor extends Actor {
    import ParentActor._
    import ChildActor._

    override def receive: Receive = happyReceive

    def happyReceive: Receive = {
      case Food(VEGETABLE) =>
        println("[child] was happy, but now eating vegetable")
        context.become(sadReceive, false)
      case Food(CHOCOLATE) =>
        println("[child] still happy, eating choc")
        context.unbecome()
      case Ask(_) => sender ! RequestAccept
    }

    def sadReceive: Receive = {
      case Food(VEGETABLE) =>
        println("[child] still sad, eating vegetable")
        context.become(sadReceive, false)
      case Food(CHOCOLATE) =>
        println("[child] was sad, eating choc")
        context.unbecome()
      case Ask(_) => sender ! RequestReject
    }
  }

  object ChildActor {
    case object RequestAccept
    case object RequestReject
  }


  class ParentActor extends Actor {
    import ParentActor._
    import ChildActor._

    override def receive: Receive = {
      case Init(ref) =>
        ref ! Food(VEGETABLE)
        ref ! Food(VEGETABLE)
        ref ! Food(CHOCOLATE)
        ref ! Ask("lol?")
      case RequestAccept => println("[parent] good")
      case RequestReject => println("[parent] bad")
    }
  }

  object ParentActor {
    case class Init(ref: ActorRef)
    case class Food(food: String)
    case class Ask(message: String)
    val VEGETABLE = "vegetable"
    val CHOCOLATE = "chocolate"
  }

  val system = ActorSystem("actorBehaviour")
  val statelessActor = system.actorOf(Props[ChildActor])
  val parentActor = system.actorOf(Props[ParentActor])

  parentActor ! ParentActor.Init(statelessActor)
}
