package akka.actors

import akka.actor.{Actor, ActorLogging, ActorRef, Props}

private class WordCountWorker extends Actor with ActorLogging {
  import WordCountWorker._
  override def receive: Receive = {
    case WorkerRequest(id, text) =>
      log.info(s"""counting words in message "$text" for request id $id""")
      sender() ! WorkerResponse(id, text.split(" ").length)
  }
}

private object WordCountWorker {
  final case class WorkerRequest(id: Int, text: String)
  final case class WorkerResponse(id: Int, count: Int)
}


class DistributedWordCounter extends Actor with ActorLogging {
  import DistributedWordCounter._

  override def receive: Receive = {
    case Initialise(n) =>
      log.info(s"initialising DistributedWordCounter with $n workers")
      val workers = (0 until n).map(i => context.actorOf(Props[WordCountWorker], s"worker-$i")).toList
      context.become(withWorkers(workers, 0, Map()))
    case _ =>
      log.warning(s"unable to perform any operations until initialised")
  }

  def withWorkers(workers: List[ActorRef], id: Int, results: Map[Int, Int]): Receive = {
    case WordCountWorker.WorkerResponse(id, count) =>
      log.info(s"operation id $id returned with count $count")
      context.become(withWorkers(workers, id, results + (id -> count)))
    case WordCountRequest(text) =>
      log.info(s"""received request id $id to count words in "$text"""")
      workers.head ! WordCountWorker.WorkerRequest(id, text)
      sender() ! WordCountResponse(id)
      context.become(withWorkers(workers.tail :+ workers.head, id+1, results + (id -> -1)))
    case WordCountEnquiryRequest(id) => results.get(id) match {
      case Some(-1) =>
        log.info(s"request id $id is still being processed")
      case Some(count) =>
        log.info(s"request id $id resulted in $count")
        sender() ! WordCountEnquiryResponse(id, Some(count))
      case None =>
        log.info(s"request id $id does not exist")
        sender() ! WordCountEnquiryResponse(id, None)
    }
  }
}

object DistributedWordCounter {
  final case class Initialise(nChildren: Int)
  final case class WordCountRequest(text: String)
  final case class WordCountResponse(id: Int)
  final case class WordCountEnquiryRequest(id: Int)
  final case class WordCountEnquiryResponse(id: Int, count: Option[Int])

  def props: Props = Props(new DistributedWordCounter())
}
