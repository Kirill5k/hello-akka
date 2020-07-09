package akka.infrastructure

import akka.actor.{Actor, ActorLogging, ActorSystem, Cancellable, Props, Timers}

import scala.concurrent.duration._
import scala.language.postfixOps

object Infra1TimersSchedulers extends App {

  class SimpleActor extends Actor with ActorLogging {
    override def receive: Receive = {
      case message => log.info(message.toString)
    }
  }

  class SelfclosingActor extends Actor with ActorLogging {
    override def receive: Receive = withSchedule(createTimeoutWindow())

    def withSchedule(schedule: Cancellable): Receive = {
      case "timeout" =>
        log.info("stopping myself")
        context.stop(self)
      case message =>
        log.info(s"received $message. staying al;ive")
        schedule.cancel()
        context.become(withSchedule(createTimeoutWindow()))
    }

    def createTimeoutWindow(): Cancellable = {
      context.system.scheduler.scheduleOnce(1 second) {
        self ! "timeout"
      }(context.system.dispatcher)
    }
  }

  object SelfclosingActorWithTimer {
    case object TimerStart
    case object TimerKey
    case object TimerReminder
    case object TimerStop
  }
  class SelfclosingActorWithTimer extends Actor with ActorLogging with Timers {
    import SelfclosingActorWithTimer._
    timers.startSingleTimer(TimerKey, TimerStart, 500 millis)

    override def receive: Receive = {
      case TimerStart =>
        log.info("bootstrapping")
        timers.startPeriodicTimer(TimerKey, TimerReminder, 1 second)
      case TimerReminder =>
        log.info("i am alive")
      case TimerStop =>
        log.warning("stopping")
        timers.cancel(TimerKey)
        context.stop(self)
    }
  }

  val system = ActorSystem("Infra1TimersSchedulers")
  val simpleActor = system.actorOf(Props[SimpleActor], "simple")
  val selfclosingActor = system.actorOf(Props[SelfclosingActor], "selfclosing")
  val selfclosingActorWithTimer = system.actorOf(Props[SelfclosingActorWithTimer], "selfclosingWithTimer")
  implicit val dispatcher = system.dispatcher

  //  system.log.info("scheduling reminder for simpleActor")
  //
  //  system.scheduler.scheduleOnce(1 second) {
  //    simpleActor ! "reminder"
  //  }
  //
  //  val routine: Cancellable = system.scheduler.schedule(1 second, 2 seconds) {
  //    simpleActor ! "heartbeat"
  //  }
  //
  //  system.scheduler.scheduleOnce(5 seconds) {
  //    routine.cancel()
  //  }

//  system.scheduler.scheduleOnce(250 millisecond) {
//    selfclosingActor ! "ping"
//  }
//
//  system.scheduler.scheduleOnce(2 seconds) {
//    selfclosingActor ! "pong"
//  }

  system.scheduler.scheduleOnce(5 seconds) {
    selfclosingActorWithTimer ! SelfclosingActorWithTimer.TimerStop
  }
}
