package akka.actors

import akka.actor.{Actor, ActorLogging, Props}

class Counter(private val initialCount: Int) extends Actor with ActorLogging {
  import Counter._

  def withCount(i: Int): Receive = {
    case Increment => context.become(withCount(i+1))
    case Decrement => context.become(withCount(i-1))
    case Show => log.info(s"current count is $i")
  }

  override def receive: Receive = withCount(initialCount)
}

object Counter {
  final case object Increment
  final case object Decrement
  final case object Show

  def props: Props = Props(new Counter(0))
}
