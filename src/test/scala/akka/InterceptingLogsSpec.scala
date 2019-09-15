package akka

import akka.InterceptingLogsSpec.CheckoutActor
import akka.InterceptingLogsSpec.CheckoutActor.{AuthorizeCardFail, AuthorizeCardRequest, AuthorizeCardSuccess, CheckoutItem, DispatchOrder, OrderConfirmed}
import akka.actor.{Actor, ActorLogging, ActorSystem, Props}
import akka.testkit.{EventFilter, ImplicitSender, TestKit}
import com.typesafe.config.ConfigFactory
import org.scalatest.{BeforeAndAfterAll, WordSpecLike}

class InterceptingLogsSpec extends TestKit(ActorSystem("InterceptingLogsSpec", ConfigFactory.load().getConfig("interceptingLogMessages")))
  with ImplicitSender with WordSpecLike with BeforeAndAfterAll {
  override def afterAll(): Unit = {
    TestKit.shutdownActorSystem(system)
  }

  "Checkout actor" should {
    val actor = system.actorOf(Props[CheckoutActor])
    val item = "iphone"
    val creditCard = "1234123412341234"
    val invalidCreditCard = "0000000000000000"
    "correctly log the dispatch of an order" in {
      EventFilter.info(pattern = s"order id [0-9]+ for item $item has been dispatched", occurrences = 1) intercept {
        actor ! CheckoutItem(item, creditCard)
      }
    }

    "catches exception when order denied" in {
      EventFilter[RuntimeException](occurrences = 1) intercept {
        actor ! CheckoutItem(item, invalidCreditCard)
      }
    }
  }
}

object InterceptingLogsSpec {
  object CheckoutActor {
    case class CheckoutItem(item: String, creditCard: String)
    case class AuthorizeCardRequest(creditCard: String)
    case object AuthorizeCardSuccess
    case object AuthorizeCardFail
    case class DispatchOrder(item: String)
    case object OrderConfirmed
  }

  class CheckoutActor extends Actor {
    private val paymentManager = context.actorOf(Props[PaymentManager])
    private val fulfillmentManager = context.actorOf(Props[FulfillmentManager])

    override def receive: Receive = awaitingCheckout

    def awaitingCheckout: Receive = {
      case CheckoutItem(item, creditCard) =>
        paymentManager ! AuthorizeCardRequest(creditCard)
        context.become(pendingPayment(item))
    }

    def pendingPayment(item: String): Receive = {
      case AuthorizeCardSuccess =>
        fulfillmentManager ! DispatchOrder(item)
        context.become(pendingFulfillment(item))
      case AuthorizeCardFail =>
        throw new RuntimeException("credit card denied")
    }

    def pendingFulfillment(item: String): Receive = {
      case OrderConfirmed => context.become(awaitingCheckout)
    }
  }

  class PaymentManager extends Actor {
    override def receive: Receive = {
      case AuthorizeCardRequest(card) =>
        if (card.startsWith("0")) sender ! AuthorizeCardFail
        else sender ! AuthorizeCardSuccess
    }
  }

  class FulfillmentManager extends Actor with ActorLogging {
    override def receive: Receive = processOrder(0)

    def processOrder(orderId: Int): Receive = {
      case DispatchOrder(item) =>
        log.info("order id {} for item {} has been dispatched", orderId, item)
        sender ! OrderConfirmed
        context.become(processOrder(orderId+1))
    }
  }
}
