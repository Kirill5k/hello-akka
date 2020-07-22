package akka.actors

import akka.actor.{Actor, Props}

class BankAccount(private val initialBalance: Int) extends Actor {
  import BankAccount._

  def withBalance(balance: Int): Receive = {
    case Deposit(amount) if amount <= 0 =>
      sender() ! Failure(s"[bank] invalid deposit amount $amount")
    case Deposit(amount) =>
      val newBalance = balance + amount
      sender() ! Success(s"[bank] successfully deposited $amount. current balance is $newBalance")
      context.become(withBalance(newBalance))
    case Withdraw(amount) if amount <= 0 =>
      sender() ! Failure(s"[bank] invalid withdraw amount $amount")
    case Withdraw(amount) =>
      val newBalance = balance - amount
      if (newBalance < 0) {
        sender() ! Failure(s"[bank] insufficient funds to withdraw, current balance is $balance")
      } else {
        sender() ! Success(s"[bank] successfully withdrawn $amount. current balance is $newBalance")
        context.become(withBalance(newBalance))
      }
    case Statement =>
      sender() ! Success(s"[bank] current balance is $balance")
  }

  override def receive: Receive = withBalance(initialBalance)
}

object BankAccount {
  final case class Deposit(amount: Int)
  final case class Withdraw(amount: Int)
  final case object Statement
  final case class Success(message: String)
  final case class Failure(message: String)

  def props(initialBalance: Int): Props =
    Props(new BankAccount(initialBalance))
}
