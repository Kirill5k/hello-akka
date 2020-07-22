package akka.actors

class BankAccountSpec extends AkkaSpec("BankAccountSpec") {

  "A BackAccount actor" should {
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
  }
}
