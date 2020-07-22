package akka.actors

import akka.actor.ActorSystem
import akka.testkit.{ImplicitSender, TestKit}
import org.scalatest.BeforeAndAfterAll
import org.scalatest.wordspec.AnyWordSpecLike

abstract class AkkaSpec(private val specName: String)
    extends TestKit(ActorSystem(specName)) with ImplicitSender with AnyWordSpecLike
    with BeforeAndAfterAll {

  override def afterAll(): Unit =
    TestKit.shutdownActorSystem(system)
}
