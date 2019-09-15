package akka

import akka.actor.{Actor, ActorSystem, Props}
import akka.testkit.{ImplicitSender, TestKit}
import org.scalatest.{BeforeAndAfterAll, WordSpecLike}

import scala.concurrent.duration._
import scala.util.Random

import BasicActorSpec._

class BasicActorSpec extends TestKit(ActorSystem("BasicSpec")) with ImplicitSender with WordSpecLike with BeforeAndAfterAll {
  override def afterAll(): Unit = {
    TestKit.shutdownActorSystem(system)
  }

  "Basic actor" should {
    "send back the same message" in {
      val basicActor = system.actorOf(Props[BasicActor])
      val message = "hello, test!"
      basicActor ! message

      expectMsg(message)
    }
  }

  "Blackhole actor" should {
    "not send back the same message" in {
      val blackholeActor = system.actorOf(Props[BlackholeActor])
      val message = "hello, test!"
      blackholeActor ! message

      expectNoMessage(5 seconds)
    }
  }

  "StringTransformationActor actor" should {
    val actor = system.actorOf(Props[StringTransformationActor])

    "reply with uppercase" in {
      actor ! "hello, test!"
      val response = expectMsgType[String]
      assert(response == "HELLO, TEST!")
    }

    "respond to a greeting" in {
      actor ! "greeting"
      expectMsgAnyOf("Hi", "Hello")
    }

    "response with a favorite tech" in {
      actor ! "favorite-tech"
      expectMsgAllOf("scala", "akka")
    }

    "response with a favorite tec in a different way" in {
      actor ! "favorite-tech"
      val messages = receiveN(2)
      assert(messages.contains("scala"))
    }
  }
}

object BasicActorSpec {
  class BasicActor extends Actor {
    override def receive: Receive = {
      case message => sender ! message
    }
  }

  class BlackholeActor extends Actor {
    override def receive: Receive = {
      case message => println(message.toString)
    }
  }

  class StringTransformationActor extends Actor {
    private val random = Random
    override def receive: Receive = {
      case "greeting" => if (random.nextBoolean()) sender ! "Hi" else sender ! "Hello"
      case "favorite-tech" =>
        sender ! "akka"
        sender ! "scala"
      case message => sender ! message.toString.toUpperCase
    }
  }
}
