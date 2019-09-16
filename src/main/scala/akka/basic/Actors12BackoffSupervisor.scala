package akka.basic

import java.io.File

import akka.actor.SupervisorStrategy.Stop
import akka.actor.{Actor, ActorLogging, ActorSystem, OneForOneStrategy, Props}
import akka.basic.Actors12BackoffSupervisor.FilebasedPersistentActor.ReadFile
import akka.pattern.{Backoff, BackoffSupervisor}

import scala.io.Source
import scala.concurrent.duration._

object Actors12BackoffSupervisor extends App {
  val system = ActorSystem("system")

  object FilebasedPersistentActor {
    case object ReadFile
  }

  class EagerFilebasedPersistentActor extends Actor with ActorLogging {
    private val ImportantFile = "src/main/resources/testfiles/important2.txt"
    import FilebasedPersistentActor._

    override def receive: Receive = withDataSource(Source.fromFile(new File(ImportantFile)))

    def withDataSource(source: Source): Receive = {
      case ReadFile => log.info(s"reading file ${source.getLines().toList}")
    }

    override def preStart(): Unit = {
      log.info("eager persistent actor starting")
    }

    override def preRestart(reason: Throwable, message: Option[Any]): Unit = {
      log.warning("eager persistent actor restarting")
    }

    override def postStop(): Unit = {
      log.info("eager persistent actor stopped")
    }
  }

  class FilebasedPersistentActor extends Actor with ActorLogging {
    private val ImportantFile = "src/main/resources/testfiles/important2.txt"
    import FilebasedPersistentActor._

    override def receive: Receive = {
      case ReadFile =>
        val source = Source.fromFile(new File(ImportantFile))
        log.info(s"reading file ${source.getLines().toList}")
    }

    override def preStart(): Unit = {
      log.info("persistent actor starting")
    }

    override def preRestart(reason: Throwable, message: Option[Any]): Unit = {
      log.warning("persistent actor restarting")
    }

    override def postStop(): Unit = {
      log.info("persistent actor stopped")
    }
  }

//  val persistentActor = system.actorOf(Props[FilebasedPersistentActor], "persistentActor")
//  persistentActor ! ReadFile

  val simpleSupervisorProps = BackoffSupervisor.props(
    Backoff.onFailure(
      Props[FilebasedPersistentActor],
      "simpleBackoffActor",
      3 seconds,
      30 seconds,
      0.2
    )
  )

//  val simpleBackoffSupervisor = system.actorOf(simpleSupervisorProps, "simpleSupervisor")
//  simpleBackoffSupervisor ! ReadFile

  val stopSupervisorProps = BackoffSupervisor.props(
    Backoff.onStop(
      Props[FilebasedPersistentActor],
      "stopBackoffActor",
      3 seconds,
      30 seconds,
      0.2
    ).withSupervisorStrategy(
      OneForOneStrategy() {
        case _ => Stop
      }
    )
  )

//  val stopBackoffSupervisor = system.actorOf(stopSupervisorProps, "stopSupervisor")
//  stopBackoffSupervisor ! ReadFile

  val repeatedSupervisorProps = BackoffSupervisor.props(
    Backoff.onStop(
      Props[EagerFilebasedPersistentActor],
      "eagerActor",
      3 seconds,
      30 seconds,
      0.2
    )
  )

  val repeatedSupervisor = system.actorOf(repeatedSupervisorProps, "eagerSupervisor")
}
