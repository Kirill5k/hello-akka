package akka.basic

import akka.actor.{Actor, ActorRef, ActorSystem, Props}
import akka.basic.Actors7ChildActorsExercises.WordCounterMaster.{Initialize, WordCountResponse, WordCountTask}

object Actors7ChildActorsExercises extends App {
  val system = ActorSystem("system")

  object WordCounterMaster {
    case class Initialize(nChildren: Int)
    case class WordCountTask(id: Int, text: String)
    case class WordCountResponse(id: Int, count: Int)
  }

  class WordCounterMaster extends Actor {
    import WordCounterMaster._

    override def receive: Receive = withNoWorkers

    def withNoWorkers: Receive = {
      case Initialize(n) =>
        val workers = (0 until n).map(i => context.actorOf(Props[WordCounterWorker], s"worker-$i"))
        println(s"${self.path} created $n workers")
        context.become(withWorkers(workers, 0, 0))
      case _ => println(s"${self.path} not yet initialized")
    }

    def withWorkers(workers: IndexedSeq[ActorRef], currentWorker: Int, currentTaskId: Int): Receive = {
      case text: String =>
        println(s"${self.path} creating task $currentTaskId for $text")
        workers(currentWorker) ! WordCountTask(currentTaskId, text)
        context.become(withWorkers(workers, (currentWorker+1)%workers.size, currentTaskId+1))
      case WordCountResponse(id, count) => println(s"${self.path} task id $id consists of $count words")
    }
  }

  class WordCounterWorker extends Actor {
    override def receive: Receive = {
      case WordCountTask(id, text) =>
        println(s"${self.path} counting words in $text")
        sender ! WordCountResponse(id, text.split(" ").length)
    }
  }

  val master = system.actorOf(Props[WordCounterMaster], "master")

  master ! Initialize(5)
  master ! "scala is decent"
  master ! "akka is kind of decent as well"
  master ! "another message"
  master ! "one more message"
  master ! "yet another message"
  master ! "just few more left"
  master ! "almost over"
  master ! "this is the end"
  master ! "yes"
}
