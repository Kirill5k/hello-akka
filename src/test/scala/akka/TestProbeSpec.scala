package akka

import akka.actor.{Actor, ActorRef, ActorSystem, Props}
import akka.testkit.{ImplicitSender, TestKit, TestProbe}
import org.scalatest.{BeforeAndAfterAll, WordSpecLike}

class TestProbeSpec extends TestKit(ActorSystem("TestProbeSpec")) with ImplicitSender with WordSpecLike with BeforeAndAfterAll {
  override def afterAll(): Unit = {
    TestKit.shutdownActorSystem(system)
  }

  import TestProbeSpec._
  import TestProbeSpec.MasterActor._

  "A master actor" should {
    val master = system.actorOf(Props[MasterActor])
    val slave = TestProbe("slave")
    val text = "i like akka"

    "register a slave" in {
      master ! RegistrationRequest(slave.ref)
      expectMsg(RegistrationResponse)
    }

    "send work to a slave" in {
      master ! Work(text)
      slave.expectMsg(SlaveRequest(text, testActor))
    }

    "receive replay from slave" in {
      slave.reply(SlaveResponse(3, testActor))

      expectMsg(Report(3))
    }

    "aggregate data correctly" in {
      master ! Work(text)
      master ! Work(text)

      slave.receiveWhile() {
        case SlaveRequest(`text`, `testActor`) => slave.reply(SlaveResponse(3, testActor))
      }

      expectMsg(Report(6))
      expectMsg(Report(9))
    }
  }
}

object TestProbeSpec {
  object MasterActor {
    case class RegistrationRequest(ref: ActorRef)
    case object RegistrationResponse
    case class Work(text: String)
    case class SlaveRequest(text: String, requester: ActorRef)
    case class SlaveResponse(count: Int, requester: ActorRef)
    case class Report(count: Int)
  }
  class MasterActor extends Actor {
    import MasterActor._

    override def receive: Receive = {
      case RegistrationRequest(slaveRef: ActorRef) =>
        sender ! RegistrationResponse
        context.become(online(slaveRef, 0))
      case _ =>
    }

    def online(slaveRef: ActorRef, i: Int): Receive = {
      case Work(text) => slaveRef ! SlaveRequest(text, sender())
      case SlaveResponse(count, requester) =>
        val newCount = i + count
        requester ! Report(newCount)
        context.become(online(slaveRef, newCount))
    }

  }

  class SlaveActor extends Actor {
    import MasterActor._
    override def receive: Receive = {
      case SlaveRequest(text, requester) => sender() ! SlaveResponse(text.split(" ").length, requester)
    }
  }
}
