package akka.basic

import akka.actor.{Actor, ActorRef, ActorSystem, Props}
import akka.actors.{BankAccount, Counter}
import akka.basic.Actors3Exercises.AccountHolder.LiveTheLife

object Actors3Exercises extends App {

  class AccountHolder extends Actor {
    import AccountHolder._
    override def receive: Receive = {
      case LiveTheLife(account) =>
        account ! BankAccount.Deposit(100)
        account ! BankAccount.Withdraw(200)
        account ! BankAccount.Withdraw(50)
        account ! BankAccount.Statement
      case message => println(message.toString)
    }
  }

  object AccountHolder {
    final case class LiveTheLife(account: ActorRef)
  }

  val system = ActorSystem("System")
  val counter = system.actorOf(Counter.props, "counter")
  val bank = system.actorOf(BankAccount.props(0), "bank")
  val accountHolder = system.actorOf(Props[AccountHolder], "account")

  counter ! Counter.Increment
  counter ! Counter.Increment
  counter ! Counter.Increment
  counter ! Counter.Decrement
  counter ! Counter.Show

  accountHolder !  LiveTheLife(bank)
}
