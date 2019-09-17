package akka.infrastructure

import akka.actor.{Actor, ActorLogging, ActorSystem, Props}
import com.typesafe.config.ConfigFactory

import scala.concurrent.{ExecutionContext, Future}
import scala.util.Random

object Infra3Dispatchers extends App {
  val rand = Random
  val system = ActorSystem("Infra3Dispatchers") //, ConfigFactory.load().getConfig("dispatchersDemo")

  class Counter extends Actor with ActorLogging {

    def withCount(count: Int): Receive = {
      case message =>
        log.info(s"[$count] $message")
        context.become(withCount(count+1))
    }

    override def receive: Receive = withCount(1)
  }

  class DBActor extends Actor with ActorLogging {
//    implicit val executionContext: ExecutionContext = context.dispatcher
    implicit val executionContext: ExecutionContext = context.system.dispatchers.lookup("my-dispatcher")

    override def receive: Receive = {
      case message => Future {
        Thread.sleep(5000)
        log.info(s"success. $message")
      }
    }
  }

//  val counters = (1 to 10).map(i => system.actorOf(Props[Counter].withDispatcher("my-dispatcher"), s"counter-$i"))
//  for (i <- 1 to 1000) {
//    counters(rand.nextInt(10)) ! i
//  }

  val dbactor = system.actorOf(Props[DBActor], "dbactor")
  val nonblockingactor = system.actorOf(Props[Counter], "nonblock")
  for (i <- 1 to 1000) {
    val message = s"important message $i"
    dbactor ! message
    nonblockingactor ! message
  }
}
