package akka.basic

import akka.actor.{Actor, ActorSystem, Props}
import akka.actors.WordCounter

object Actors1Intro extends App {

  val actorSystem = ActorSystem("firstActorSystem")
  println(actorSystem.name)

  val wordCountActor = actorSystem.actorOf(WordCounter.props(1), "wordCounter")
  val anotherWordCountActor = actorSystem.actorOf(WordCounter.props(2), "anotherWordCounter")

  wordCountActor ! "passing a message to my actor"
  anotherWordCountActor ! "passing another message to my other actor"
  wordCountActor ! 1
}
