package akka.patterns

import akka.actor.{Actor, ActorLogging, ActorRef, ActorSystem, Cancellable, FSM, Props}
import akka.patterns.FiniteStateMachineSpec.CoolVendingMachine.{VendingData, VendingState}
import akka.testkit.{ImplicitSender, TestKit}
import org.scalatest.BeforeAndAfterAll
import org.scalatest.wordspec.AnyWordSpecLike

import scala.concurrent.duration._
import scala.language.postfixOps

class FiniteStateMachineSpec extends TestKit(ActorSystem("FiniteStateMachineSpec")) with ImplicitSender with AnyWordSpecLike with BeforeAndAfterAll {
  import FiniteStateMachineSpec._
  import VendingMachine._

  override def afterAll(): Unit = {
    TestKit.shutdownActorSystem(system)
  }

  def runTestSuite(props: Props): Unit = {
    val vendingMachine = system.actorOf(props)

    "error when not initialised" in {
      vendingMachine ! RequestProduct("cheese")
      expectMsg(VendingError("not-initialised"))
    }

    "report product not available" in {
      vendingMachine ! Initialise(Map("coke" -> 10), Map("coke" -> 5))

      vendingMachine ! RequestProduct("cheese")
      expectMsg(VendingError("product-not-available"))
    }

    "throws a timeout if I dont insert money" in {
      vendingMachine ! RequestProduct("coke")
      expectMsg(Instruction("please insert 5 dollars"))

      within(2.5 seconds) {
        expectMsg(VendingError("request-timeout"))
      }
    }

    "handle reception of partial money" in {
      vendingMachine ! RequestProduct("coke")
      vendingMachine ! ReceiveMoney(3)
      expectMsgAllOf(Instruction("please insert 5 dollars"), Instruction("please insert 2 dollars more"))

      within(2.5 seconds) {
        expectMsgAllOf(GiveBackChange(3), VendingError("request-timeout"))
      }
    }

    "deliver a product and issue change" in {
      vendingMachine ! RequestProduct("coke")
      vendingMachine ! ReceiveMoney(6)
      expectMsgAllOf(Instruction("please insert 5 dollars"), Deliver("coke"), GiveBackChange(1))
    }
  }

  "A simple vending machine" should {
    runTestSuite(Props[VendingMachine])
  }

  "A FSM vending machine" should {
    runTestSuite(Props[CoolVendingMachine])
  }
}

object FiniteStateMachineSpec {
  object VendingMachine {
    case class Initialise(inventory: Map[String, Int], prices: Map[String, Int])
    case class RequestProduct(product: String)
    case class Instruction(instruction: String)
    case class ReceiveMoney(amount: Int)
    case class Deliver(product: String)
    case class GiveBackChange(amount: Int)

    case class VendingError(message: String)
    case object ReceiveMoneyTimeout
  }

  class VendingMachine extends Actor with ActorLogging {
    import VendingMachine._

    override def receive: Receive = idle

    def idle: Receive = {
      case Initialise(items, prices) => context.become(operational(items, prices))
      case _ =>
        log.warning("[idle] unexpected message")
        sender() ! VendingError("not-initialised")
    }

    def operational(inventory: Map[String, Int], prices: Map[String, Int]): Receive = {
      case RequestProduct(product) => inventory.get(product) match {
        case None | Some(0) =>
          log.warning("[operational] product not in stock")
          sender() ! VendingError("product-not-available")
        case Some(_) =>
          val price = prices(product)
          sender() ! Instruction(s"please insert $price dollars")
          context.become(waitingForMoney(inventory, prices, product, 0, startReceiveMoneyTimeoutSchedule, sender()))
      }
    }

    def waitingForMoney(
                         inventory: Map[String, Int],
                         prices: Map[String, Int],
                         product: String,
                         money: Int,
                         moneyTimeoutSchedule: Cancellable,
                         requester: ActorRef
                       ): Receive = {
      case ReceiveMoneyTimeout =>
        log.warning("[waitingForMoney] receive timeout")
        requester ! VendingError("request-timeout")
        if (money > 0) requester ! GiveBackChange(money)
        context.become(operational(inventory, prices))
      case ReceiveMoney(amount) =>
        moneyTimeoutSchedule.cancel()
        val price = prices(product)
        if (money + amount >= price) {
          requester ! Deliver(product)
          if (money + amount - price > 0) requester ! GiveBackChange(money+amount-price)
          context.become(operational(inventory + (product -> (inventory(product)-1)), prices))
        } else {
          val remainingMoney = price - money - amount
          requester ! Instruction(s"please insert $remainingMoney dollars more")
          context.become(waitingForMoney(inventory, prices, product, money+amount, startReceiveMoneyTimeoutSchedule, requester))
        }
      case _ =>
        log.warning("[waitingForMoney] unexpected message")
        sender() ! VendingError("not-initialised")
    }

    def startReceiveMoneyTimeoutSchedule: Cancellable = context.system.scheduler.scheduleOnce(2 seconds){
      self ! ReceiveMoneyTimeout
    }(context.dispatcher)
  }

  object CoolVendingMachine {
    trait VendingState
    case object Idle extends VendingState
    case object Operational extends VendingState
    case object WaitForMoney extends VendingState

    trait VendingData
    case object Uninitialized extends VendingData
    case class Initialized(inventory: Map[String, Int], prices: Map[String, Int]) extends VendingData
    case class WaitingForMoney(inventory: Map[String, Int], prices: Map[String, Int], product: String, money: Int, requester: ActorRef) extends VendingData
  }

  class CoolVendingMachine extends FSM[VendingState, VendingData] {
    import CoolVendingMachine._
    import VendingMachine._

    startWith(Idle, Uninitialized)

    when(Idle) {
      case Event(Initialise(inventory, prices), Uninitialized) => goto(Operational) using Initialized(inventory, prices)
      case _ =>
        sender() ! VendingError("not-initialised")
        stay()
    }

    when(Operational) {
      case Event(RequestProduct(product), Initialized(inventory, prices)) => inventory.get(product) match {
        case None | Some(0) =>
          log.warning("[operational] product not in stock")
          sender() ! VendingError("product-not-available")
          stay()
        case Some(_) =>
          val price = prices(product)
          sender() ! Instruction(s"please insert $price dollars")
          goto(WaitForMoney) using WaitingForMoney(inventory, prices, product, 0, sender())
      }
    }

    when(WaitForMoney, stateTimeout = 2 seconds) {
      case Event(StateTimeout,  WaitingForMoney(inventory, prices, product, money, requester)) =>
        log.warning("[waitingForMoney] receive timeout")
        requester ! VendingError("request-timeout")
        if (money > 0) requester ! GiveBackChange(money)
        goto(Operational) using Initialized(inventory, prices)
      case Event(ReceiveMoney(amount), WaitingForMoney(inventory, prices, product, money, requester)) =>
        val price = prices(product)
        if (money + amount >= price) {
          requester ! Deliver(product)
          if (money + amount - price > 0) requester ! GiveBackChange(money+amount-price)
          goto(Operational) using Initialized(inventory + (product -> (inventory(product)-1)), prices)
        } else {
          val remainingMoney = price - money - amount
          requester ! Instruction(s"please insert $remainingMoney dollars more")
          stay() using WaitingForMoney(inventory, prices, product, money+amount, requester)
        }
    }

    whenUnhandled {
      case Event(_, _) =>
        sender() ! VendingError("command-not-found")
        stay()
    }

    onTransition {
      case stateA -> stateB => log.info(s"transitioning from $stateA to $stateB")
    }
  }
}
