package akka.actors

class BankAccountSpec extends AkkaSpec("BankAccountSpec") {

  "A BackAccount actor" should {
    "on statement" should {
      "return current balance" in {
        val backAccount = system.actorOf(BankAccount.props(100))

        backAccount ! BankAccount.Statement

        expectMsg(BankAccount.Success(s"[bank] current balance is 100"))
      }
    }

    "on deposit" should {
      "return success if deposit amount is valid" in {
        val backAccount = system.actorOf(BankAccount.props(100))

        backAccount ! BankAccount.Deposit(100)

        expectMsg(BankAccount.Success(s"[bank] successfully deposited 100. current balance is 200"))
      }

      "return failure if deposit amount is less than 0" in {
        val backAccount = system.actorOf(BankAccount.props(100))

        backAccount ! BankAccount.Deposit(-100)

        expectMsg(BankAccount.Failure(s"[bank] invalid deposit amount -100"))
      }

      "accumulate balance" in {
        val backAccount = system.actorOf(BankAccount.props(100))

        backAccount ! BankAccount.Deposit(100)
        backAccount ! BankAccount.Deposit(200)

        expectMsgAllOf(
          BankAccount.Success(s"[bank] successfully deposited 100. current balance is 200"),
          BankAccount.Success(s"[bank] successfully deposited 200. current balance is 400")
        )
      }
    }

    "on withdraw" should {
      "return failure if withdraw amount is less than 0" in {
        val backAccount = system.actorOf(BankAccount.props(100))

        backAccount ! BankAccount.Withdraw(-50)

        expectMsg(BankAccount.Failure(s"[bank] invalid withdraw amount -50"))
      }

      "return success and update balance" in {
        val backAccount = system.actorOf(BankAccount.props(100))

        backAccount ! BankAccount.Withdraw(50)

        expectMsg(BankAccount.Success(s"[bank] successfully withdrawn 50. current balance is 50"))
      }

      "return failure when there is not enough money on the account" in {
        val backAccount = system.actorOf(BankAccount.props(100))

        backAccount ! BankAccount.Withdraw(200)

        expectMsg(BankAccount.Failure(s"[bank] insufficient funds to withdraw, current balance is 100"))
      }

      "update state on multiple withdrawals" in {
        val backAccount = system.actorOf(BankAccount.props(100))

        backAccount ! BankAccount.Withdraw(50)
        backAccount ! BankAccount.Withdraw(20)

        expectMsgAllOf(
          BankAccount.Success(s"[bank] successfully withdrawn 50. current balance is 50"),
          BankAccount.Success(s"[bank] successfully withdrawn 20. current balance is 30")
        )
      }
    }
  }
}
