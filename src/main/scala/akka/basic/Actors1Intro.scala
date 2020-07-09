package akka.basic

import akka.actor.{Actor, ActorSystem, Props}
import akka.actors.WordCountActor

object Actors1Intro extends App {

  val actorSystem = ActorSystem("firstActorSystem")
  println(actorSystem.name)

  val wordCountActor = actorSystem.actorOf(WordCountActor.props(1), "wordCounter")
  val anotherWordCountActor = actorSystem.actorOf(WordCountActor.props(2), "anotherWordCounter")

  wordCountActor ! "passing a message to my actor"
  anotherWordCountActor ! "passing another message to my other actor"
  wordCountActor ! 1
}
