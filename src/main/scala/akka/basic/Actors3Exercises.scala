package akka.basic

import akka.actor.{Actor, ActorRef, ActorSystem, Props}
import akka.actors.{BankAccountActor, CounterActor}
import akka.basic.Actors3Exercises.AccountHolder.LiveTheLife

object Actors3Exercises extends App {

  class AccountHolder extends Actor {
    import AccountHolder._
    override def receive: Receive = {
      case LiveTheLife(account) =>
        account ! BankAccountActor.Deposit(100)
        account ! BankAccountActor.Withdraw(200)
        account ! BankAccountActor.Withdraw(50)
        account ! BankAccountActor.Statement
      case message => println(message.toString)
    }
  }

  object AccountHolder {
    final case class LiveTheLife(account: ActorRef)
  }

  val system = ActorSystem("System")
  val counter = system.actorOf(CounterActor.props, "counter")
  val bank = system.actorOf(BankAccountActor.props, "bank")
  val accountHolder = system.actorOf(Props[AccountHolder], "account")

  counter ! CounterActor.Increment
  counter ! CounterActor.Increment
  counter ! CounterActor.Increment
  counter ! CounterActor.Decrement
  counter ! CounterActor.Show

  accountHolder !  LiveTheLife(bank)
}
