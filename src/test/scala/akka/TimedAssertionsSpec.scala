package akka

import akka.TimedAssertionsSpec.WorkerActor
import akka.TimedAssertionsSpec.WorkerActor.WorkResult
import akka.actor.{Actor, ActorSystem, Props}
import akka.testkit.{ImplicitSender, TestKit}
import org.scalatest.BeforeAndAfterAll
import org.scalatest.wordspec.AnyWordSpecLike

import scala.concurrent.duration._
import scala.language.postfixOps
import scala.util.Random

class TimedAssertionsSpec extends TestKit(ActorSystem("TimedAssertionsSpec")) with ImplicitSender with AnyWordSpecLike with BeforeAndAfterAll {
  override def afterAll(): Unit = {
    TestKit.shutdownActorSystem(system)
  }

  "Worker actor" should {
    val worker = system.actorOf(Props[WorkerActor])

    "reply with the result in a timely manner" in {
      within(500 millis, 1 second) {
        worker ! "work"
        expectMsg(WorkResult(42))
      }
    }

    "reply with valid at a reasonable cadence" in {
      within(1 second) {
        worker ! "workSequence"
        val results = receiveWhile[Int](max=2 seconds, idle=500 millis, messages=10) {
          case WorkResult(result) => result
        }
        assert(results.sum > 5)
        assert(results.length == 10)
      }
    }
  }
}

object TimedAssertionsSpec {
  object WorkerActor {
    case class WorkResult(result: Int)
  }

  class WorkerActor extends Actor {
    import WorkerActor._
    private val random = Random

    override def receive: Receive = {
      case "work" =>
        Thread.sleep(500)
        sender ! WorkResult(42)
      case "workSequence" =>
        for (i <- 1 to 10) {
          Thread.sleep(random.nextInt(50))
          sender ! WorkResult(i)
        }
    }
  }
}
