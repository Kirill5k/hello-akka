package akka

import akka.actor.SupervisorStrategy.{Escalate, Restart, Resume, Stop}
import akka.actor.{Actor, ActorLogging, ActorRef, ActorSystem, AllForOneStrategy, OneForOneStrategy, Props, SupervisorStrategy, Terminated}
import akka.testkit.{EventFilter, ImplicitSender, TestKit}
import org.scalatest.{BeforeAndAfterAll, WordSpecLike}

class SupervisionSpec extends TestKit(ActorSystem("SupervisionSpec")) with ImplicitSender with WordSpecLike with BeforeAndAfterAll {
  import SupervisionSpec._

  override protected def afterAll(): Unit = TestKit.shutdownActorSystem(system)

  "A supervisor" should {
    val supervisor = system.actorOf(Props[Supervisor], "supervisor")

    "resume its child in case of minor fault" in {
      supervisor ! Props[WordCounter]
      val child = expectMsgType[ActorRef]

      child ! "Blah blah blah"
      child ! Report
      expectMsg(3)

      child ! "Blah blah blah blah blah blah blah"

      child ! Report
      expectMsg(3)
    }

    "restart its child in case empty string is sent" in {
      supervisor ! Props[WordCounter]
      val child = expectMsgType[ActorRef]

      child ! "Blah blah blah"
      child ! Report
      expectMsg(3)

      child ! ""

      child ! Report
      expectMsg(0)
    }

    "terminate its child in case of major error " in {
      supervisor ! Props[WordCounter]
      val child = expectMsgType[ActorRef]
      watch(child)

      child ! "blah blah blah"
      val terminatedMessage = expectMsgType[Terminated]
      assert(terminatedMessage.actor == child)
    }

    "escalate an error when it doesnt know what to do" in {
      supervisor ! Props[WordCounter]
      val child = expectMsgType[ActorRef]
      watch(child)

      child ! 43
      val terminatedMessage = expectMsgType[Terminated]
      assert(terminatedMessage.actor == child)
    }
  }

  "better supervisor" should {
    val supervisor = system.actorOf(Props[NoDeathOnRestartSupervisor], "bettersupervisor")

    "not kill his children" in {
      supervisor ! Props[WordCounter]
      val child = expectMsgType[ActorRef]

      child ! "Blah blah blah"
      child ! Report
      expectMsg(3)

      child ! 42
      child ! Report
      expectMsg(0)
    }
  }

  "All for one supervisor" should {
    val supervisor = system.actorOf(Props[AllForOneSupervisor], "allforonesupervisor")

    "apply all for one strategy" in {
      supervisor ! Props[WordCounter]
      val child1 = expectMsgType[ActorRef]
      supervisor ! Props[WordCounter]
      val child2 = expectMsgType[ActorRef]

      child2 ! "Testing supervision"
      child2 ! Report
      expectMsg(2)

      EventFilter[NullPointerException]() intercept {
        child1 ! ""
      }

      Thread.sleep(500)
      child2 ! Report
      expectMsg(0)
    }
  }
}

object SupervisionSpec {
  case object Report

  class Supervisor extends Actor {
    override val supervisorStrategy: SupervisorStrategy = OneForOneStrategy() {
      case _: NullPointerException => Restart
      case _: IllegalArgumentException => Stop
      case _: RuntimeException => Resume
      case _: Exception => Escalate
    }

    override def receive: Receive = {
      case props: Props => sender ! context.actorOf(props)
    }
  }

  class NoDeathOnRestartSupervisor extends Supervisor with ActorLogging {
    override def preRestart(reason: Throwable, message: Option[Any]): Unit = {
      log.info("prerestart")
    }
  }

  class AllForOneSupervisor extends Supervisor {
    override val supervisorStrategy = AllForOneStrategy() {
      case _: NullPointerException => Restart
      case _: IllegalArgumentException => Stop
      case _: RuntimeException => Resume
      case _: Exception => Escalate
    }
  }

  class WordCounter extends Actor {
    private var words = 0
    override def receive: Receive = {
      case Report => sender ! words
      case "" => throw new NullPointerException("string is empty")
      case sentence: String if sentence.length > 20 => throw new RuntimeException("string is too long")
      case sentence: String if !sentence(0).isUpper => throw new IllegalArgumentException("sentence must start with upper case")
      case sentence: String => words += sentence.split(" ").length
      case _ => throw new Exception("can only work with strings")
    }
  }
}
