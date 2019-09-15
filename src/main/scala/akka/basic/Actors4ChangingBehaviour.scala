package akka.basic

import akka.actor.{Actor, ActorRef, ActorSystem, Props}

object Actors4ChangingBehaviour extends App {

  object StatelessActor {
    case object Accept
    case object Reject
  }

  class StatelessActor extends Actor {
    import ParentActor._
    import StatelessActor._

    override def receive: Receive = happyReceive

    def happyReceive: Receive = {
      case Food(VEGETABLE) =>
        println("[stateless actor] eating vegetable")
        context.become(sadReceive, false)
      case Food(CHOCOLATE) =>
        println("[stateless actor] eating choc")
        context.unbecome()
      case Ask(_) => sender ! Accept
    }
    def sadReceive: Receive = {
      case Food(VEGETABLE) =>
        println("[stateless actor] eating vegetable")
        context.become(sadReceive, false)
      case Food(CHOCOLATE) =>
        println("[stateless actor] eating choc")
        context.unbecome()
      case Ask(_) => sender ! Reject
    }
  }

  class ParentActor extends Actor {
    import ParentActor._
    import StatelessActor._

    override def receive: Receive = {
      case Init(ref) =>
        ref ! Food(VEGETABLE)
        ref ! Food(VEGETABLE)
        ref ! Food(CHOCOLATE)
        ref ! Ask("lol?")
      case Accept => println("[parent] good")
      case Reject => println("[parent] bad")
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
  val statelessActor = system.actorOf(Props[StatelessActor])
  val parentActor = system.actorOf(Props[ParentActor])

  parentActor ! ParentActor.Init(statelessActor)
}
