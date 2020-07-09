package akka.basic

import akka.actor.{Actor, ActorSystem, Props}

object Actors1Intro extends App {

  val actorSystem = ActorSystem("firstActorSystem")
  println(actorSystem.name)

  class WordCountActor(private val number: Int) extends Actor {
    var totalWords = 0

    override def receive: PartialFunction[Any, Unit] = {
      case message: String => {
        println(s"[word count $number] received a message $message")
        totalWords += message.split(" ").length
      }
      case msg => println(s"[word counter $number] I cannot understand ${msg.toString}")
    }
  }

  object WordCountActor {
    def props(number: Int = 1) = Props(new WordCountActor(number))
  }

  val wordCountActor = actorSystem.actorOf(WordCountActor.props(), "wordCounter")
  val anotherWordCountActor = actorSystem.actorOf(WordCountActor.props(2), "anotherWordCounter")

  wordCountActor ! "passing a message to my actor"
  anotherWordCountActor ! "passing another message to my other actor"
  wordCountActor ! 1
}
