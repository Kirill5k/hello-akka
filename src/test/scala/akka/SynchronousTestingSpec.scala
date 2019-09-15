package akka

import akka.SynchronousTestingSpec._
import akka.actor.{Actor, ActorSystem, Props}
import akka.testkit.{CallingThreadDispatcher, TestActorRef, TestProbe}
import org.scalatest.{BeforeAndAfterAll, WordSpecLike}
import scala.concurrent.duration._

class SynchronousTestingSpec extends WordSpecLike with BeforeAndAfterAll {

  implicit val system = ActorSystem("SynchronousTestingSpec")

  override protected def afterAll(): Unit = {
    system.terminate()
  }

  "Counter" should {
    "syncrhonously increase its counter" in {
      val counter = system.actorOf(Props[Counter].withDispatcher(CallingThreadDispatcher.Id))
      val probe = TestProbe()

      probe.send(counter, Inc)
      probe.send(counter, Read)
      probe.expectMsg(Duration.Zero, 1)
    }
  }
}

object SynchronousTestingSpec {
  case object Inc
  case object Read
  class Counter extends Actor {
    override def receive: Receive = count(0)

    def count(i: Int): Receive = {
      case Inc => context.become(count(i+1))
      case Read => sender ! i
    }
  }
}
