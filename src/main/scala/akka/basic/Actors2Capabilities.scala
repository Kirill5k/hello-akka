package akka.basic

import akka.actor.{Actor, ActorRef, ActorSystem, Props}

object Actors2Capabilities extends App {
  class SimpleActor extends Actor {
    override def receive: Receive = {
      case "Hi" => context.sender() ! "hi to you too"
      case message: String => println(s"[simple actor $self] I have received $message")
      case number: Int => println(s"[simple actor] I have received a number $number")
      case SendMessageToYourself(content) => self ! content
      case SayHiTo(ref) => ref ! "Hi"
      case ForwardMessage(message, ref) => ref forward message
      case other => println(s"[simple actor] received unexpected message ${other.toString}")
    }
  }

  object SimpleActor {
    def props() = Props[SimpleActor]
  }

  case class SendMessageToYourself(content: String)
  case class SayHiTo(ref: ActorRef)
  case class ForwardMessage(message: String, ref: ActorRef)

  val actorSystem = ActorSystem("ActorsCapabilitiesDemo")
  val aliceActor = actorSystem.actorOf(SimpleActor.props(), "alice")
  val bobActor = actorSystem.actorOf(SimpleActor.props(), "bob")

  aliceActor ! "message to actor"
  aliceActor ! 42
  aliceActor ! SendMessageToYourself("sending message to myself")
  aliceActor ! SayHiTo(bobActor)
  aliceActor ! "Hi"
  aliceActor ! ForwardMessage("forwarding the message", bobActor)
}
