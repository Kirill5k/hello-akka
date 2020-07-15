package akka.basic

import akka.actor.{Actor, ActorRef, ActorSystem, Props}
import akka.actors.Counter
import akka.basic.Actors5Exercises.Citizen.Vote
import akka.basic.Actors5Exercises.VoteAggregator.{AggregateVotes, VoteStatusRequest, VoteStatusResponse}

object Actors5Exercises extends App {

  object Citizen {
    final case class Vote(candidate: String)

    def props(name: String): Props = Props(new Citizen(name))
  }

  class Citizen(name: String) extends Actor {
    override def receive: Receive = readyToVote

    def readyToVote: Receive = {
      case Vote(candidate) =>
        println(s"[$name] voting for $candidate")
        context.become(voted(candidate))
      case VoteStatusRequest => sender() ! VoteStatusResponse(None)
    }

    def voted(candidate: String): Receive = {
      case Vote(_) => println(s"[$name] can't vote anymore")
      case VoteStatusRequest => sender() ! VoteStatusResponse(Some(candidate))
    }
  }

  object VoteAggregator {
    final case class AggregateVotes(citizens: Set[ActorRef])
    final case object VoteStatusRequest
    final case class VoteStatusResponse(candidate: Option[String])
  }

  class VoteAggregator extends Actor {
    override def receive: Receive = aggregateVotes

    def waitForVotes(votesCount: Int, votes: Map[String, Int]): Receive = {
      case VoteStatusResponse(Some(candidate)) if votesCount > 1 =>
        println(s"[agg] vote for $candidate")
        context.become(waitForVotes(votesCount-1, votes + (candidate -> (votes.getOrElse(candidate, 0)+1))))
      case VoteStatusResponse(Some(candidate)) =>
        println(s"[agg] vote for $candidate")
        println(votes + (candidate -> (votes.getOrElse(candidate, 0)+1)))
        context.become(aggregateVotes)
      case VoteStatusResponse(None) if votesCount > 1 => context.become(waitForVotes(votesCount-1, votes))
      case VoteStatusResponse(None) =>
        println(votes)
        context.become(aggregateVotes)
      case AggregateVotes(_) => println("still aggregating votes from previous poll")
    }

    def aggregateVotes: Receive = {
      case AggregateVotes(citizens) =>
        citizens.foreach(_ ! VoteStatusRequest)
        context.become(waitForVotes(citizens.size, Map[String, Int]()))
    }
  }

  val actorSystem = ActorSystem("actorSystem")

  val alice = actorSystem.actorOf(Citizen.props("alice"), "alice")
  val bob = actorSystem.actorOf(Citizen.props("bob"), "bob")
  val charlie = actorSystem.actorOf(Citizen.props("charlie"), "charlie")
  val aggregator = actorSystem.actorOf(Props[VoteAggregator], "agg")

  alice ! Vote("Uncle Bob")
  alice ! Vote("Jonas")
  bob ! Vote("Jonas")
  charlie ! Vote("Uncle Bob")

  Thread.sleep(1000)

  aggregator ! AggregateVotes(Set(alice, bob, charlie))
}
