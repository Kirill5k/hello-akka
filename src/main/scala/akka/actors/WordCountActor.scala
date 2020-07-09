package akka.actors

import akka.actor.{Actor, Props}

class WordCountActor(private val number: Int) extends Actor {

  override def receive: Receive = withWordCount(0)

  def withWordCount(count: Int): Receive = {
    case text: String =>
      println(s"""[word-counter-$number] received a message "$text"""")
      context.become(withWordCount(count + text.split(" ").length))
    case message =>
      println(s"""[word-counter-$number] unknown message "$message"""")
  }
}

object WordCountActor {
  def props(number: Int) = Props(new WordCountActor(number))
}
