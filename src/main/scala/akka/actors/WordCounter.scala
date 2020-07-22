package akka.actors

import akka.actor.{Actor, ActorLogging, Props}

class WordCounter(private val number: Int) extends Actor with ActorLogging {

  override def receive: Receive = withWordCount(0)

  def withWordCount(count: Int): Receive = {
    case text: String =>
      log.info(s"""[word-counter-$number] received a message "$text"""")
      context.become(withWordCount(count + text.split(" ").length))
    case message =>
      log.info(s"""[word-counter-$number] unknown message "$message"""")
  }
}

object WordCounter {
  def props(number: Int) = Props(new WordCounter(number))
}
