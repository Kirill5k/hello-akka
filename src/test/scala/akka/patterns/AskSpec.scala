package akka.patterns

import akka.actor.{Actor, ActorLogging, ActorRef, ActorSystem, Props}
import akka.pattern.{ask, pipe}
import akka.testkit.{ImplicitSender, TestKit}
import akka.util.Timeout
import org.scalatest.BeforeAndAfterAll

import scala.concurrent.ExecutionContext
import scala.concurrent.duration._
import scala.language.postfixOps

import scala.util.{Failure, Success}
import AskSpec._
import AuthManager._
import KVActor._
import org.scalatest.wordspec.AnyWordSpecLike

class AskSpec extends TestKit(ActorSystem("AskSpec")) with ImplicitSender with AnyWordSpecLike with BeforeAndAfterAll {

  override def afterAll(): Unit = {
    TestKit.shutdownActorSystem(system)
  }

  "An authenticator" should {
    val authenticator = system.actorOf(Props[AuthManager], "authenticator")

    "fail to authenticate a non-registered user" in {
      authenticator ! Authenticate("sifon", "boroda")

      expectMsg(AuthFailure(AUTH_FAILURE_NOT_FOUND))
    }

    "fail to authenticate if invalid password" in {
      authenticator ! Register("sifon", "nofis")
      authenticator ! Authenticate("sifon", "boroda")

      expectMsg(AuthFailure(AUTH_FAILURE_INCORRECT_PASSWORD))
    }

    "authenticate" in {
      authenticator ! Register("sifon", "nofis")
      authenticator ! Authenticate("sifon", "nofis")

      expectMsg(AuthSuccess)
    }
  }

  "A piped authenticator" should {
    val pipedAuthenticator = system.actorOf(Props[PipedAuthManager], "pipedAuthenticator")

    "fail to authenticate a non-registered user" in {
      pipedAuthenticator ! Authenticate("sifon", "boroda")

      expectMsg(AuthFailure(AUTH_FAILURE_NOT_FOUND))
    }

    "fail to authenticate if invalid password" in {
      pipedAuthenticator ! Register("sifon", "nofis")
      pipedAuthenticator ! Authenticate("sifon", "boroda")

      expectMsg(AuthFailure(AUTH_FAILURE_INCORRECT_PASSWORD))
    }

    "authenticate" in {
      pipedAuthenticator ! Register("sifon", "nofis")
      pipedAuthenticator ! Authenticate("sifon", "nofis")

      expectMsg(AuthSuccess)
    }
  }
}

object AskSpec {
  object KVActor {
    case class Read(key: String)
    case class Write(Key: String, value: String)
  }
  class KVActor extends Actor with ActorLogging {
    import KVActor._

    override def receive: Receive = online(Map())

    def online(kv: Map[String, String]): Receive = {
      case Read(key) =>
        log.info(s"returning value for key $key")
        sender ! kv.get(key)
      case Write(key, value) =>
        log.info(s"adding key value pair: $key -> $value")
        context.become(online(kv + (key -> value)))
    }
  }

  object AuthManager {
    val AUTH_FAILURE_NOT_FOUND = "username not found"
    val AUTH_FAILURE_INCORRECT_PASSWORD = "incorrect password"
    val AUTH_FAILURE_SYSTEM_FAILURE = "system failure: "
    case class Register(username: String, password: String)
    case class Authenticate(username: String, password: String)
    case class AuthFailure(message: String)
    case object AuthSuccess
  }
  class AuthManager extends Actor with ActorLogging {
    implicit val timeout: Timeout = Timeout(1 second)
    implicit val executionContext: ExecutionContext = context.dispatcher

    override def receive: Receive = withDb(context.actorOf(Props[KVActor], "db"))

    def authenticate(db: ActorRef, username: String, password: String): Unit = {
      val originalSender = sender()
      val futureResponse = db ? Read(username)
      futureResponse.onComplete {
        case Success(None) => originalSender ! AuthFailure(AUTH_FAILURE_NOT_FOUND)
        case Success(Some(dbPassword)) if dbPassword == password => originalSender ! AuthSuccess
        case Success(Some(_)) => originalSender ! AuthFailure(AUTH_FAILURE_INCORRECT_PASSWORD)
        case Failure(exception) => originalSender ! AuthFailure(AUTH_FAILURE_SYSTEM_FAILURE + exception.getLocalizedMessage)
      }
    }

    def withDb(db: ActorRef): Receive = {
      case Register(username, password) => db ! Write(username, password)
      case Authenticate(username, password) => authenticate(db, username, password)
    }
  }

  class PipedAuthManager extends AuthManager {
    override def authenticate(db: ActorRef, username: String, password: String): Unit = {
      val future = db ? Read(username)
      val passwordFuture = future.mapTo[Option[String]]
      val responseFuture = passwordFuture.map {
        case None => AuthFailure(AUTH_FAILURE_NOT_FOUND)
        case Some(dbPassword) if dbPassword == password => AuthSuccess
        case Some(_) => AuthFailure(AUTH_FAILURE_INCORRECT_PASSWORD)
      }
      responseFuture.pipeTo(sender())
    }
  }
}

