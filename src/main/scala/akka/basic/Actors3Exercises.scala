package akka.basic

import akka.actor.{Actor, ActorRef, ActorSystem, Props}

object Actors3Exercises extends App {

  // counter actor with increment and decrement messages. print
  // bank account as an actor. deposit and withdraw. statement. replies with success or failure

  class Counter extends Actor {
    private var counter = 0
    override def receive: Receive = {
      case Counter.Increment => counter += 1
      case Counter.Decrement => counter -= 1
      case Counter.Show => println(s"[counter actor $self] current count: $counter")
    }
  }

  object Counter {
    case object Increment
    case object Decrement
    case object Show
  }

  class BankAccount extends Actor {
    import BankAccount._

    private var money = 0
    override def receive: Receive = {
      case Deposit(amount) => {
        money += amount
        sender() ! Success(s"[bank] success. current balance is $money")
      }
      case Withdraw(amount) if money - amount >= 0 => {
        money -= amount
        sender() ! Success(s"[bank] success. remaining balance is $money")
      }
      case Withdraw(_) => sender() ! Failure("[bank] failure. insufficient funds")
      case Statement => sender() ! Success(s"[bank] current balance is $money")
    }
  }

  object BankAccount {
    case class Deposit(amount: Int)
    case class Withdraw(amount: Int)
    case object Statement

    case class Success(message: String)
    case class Failure(message: String)
  }

  class AccountHolder extends Actor {
    override def receive: Receive = {
      case LiveTheLife(account) =>
        account ! Deposit(100)
        account ! Withdraw(200)
        account ! Withdraw(50)
        account ! Statement
      case message => println(message.toString)
    }
  }

  object AccountHolder {
    case class LiveTheLife(account: ActorRef)
  }

  val system = ActorSystem("System")
  val counter = system.actorOf(Props[Counter], "counter")
  val bank = system.actorOf(Props[BankAccount], "bank")
  val accountHolder = system.actorOf(Props[AccountHolder], "account")

  counter ! Counter.Increment
  counter ! Counter.Increment
  counter ! Counter.Increment
  counter ! Counter.Decrement
  counter ! Counter.Show

  accountHolder !  LiveTheLife(bank)
}
