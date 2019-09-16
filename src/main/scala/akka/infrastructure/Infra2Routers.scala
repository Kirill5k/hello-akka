package akka.infrastructure

import akka.actor.{Actor, ActorLogging, ActorRef, ActorSystem, Props, Terminated}
import akka.routing.{ActorRefRoutee, FromConfig, RoundRobinGroup, RoundRobinPool, RoundRobinRoutingLogic, Router}
import com.typesafe.config.ConfigFactory

object Infra2Routers extends App {
  class Master extends Actor {
    private val slaves = (1 to 5).map(i => {
      val slave = context.actorOf(Props[Slave], s"slave-$i")
      context.watch(slave)
      ActorRefRoutee(slave)
    })

    override def receive: Receive = withRouter(Router(RoundRobinRoutingLogic(), slaves), 6)

    def withRouter(router: Router, id: Int): Receive = {
      case Terminated(ref) =>
        val newSlave = context.actorOf(Props[Slave], s"slave-$id")
        context.watch(newSlave)
        val newRouter = router.removeRoutee(ref).addRoutee(newSlave)
        context.become(withRouter(newRouter, id+1))
      case message => router.route(message, sender())
    }
  }

  class Slave extends Actor with ActorLogging {
    override def receive: Receive = {
      case message => log.info(message.toString)
    }
  }

  val system = ActorSystem("Infra2Routers", ConfigFactory.load().getConfig("routersDemo"))
  val master = system.actorOf(Props[Master], "master")

//  for (i <- 1 to 10) {
//    master ! s"hello from world $i"
//  }

  val poolMaster = system.actorOf(RoundRobinPool(5).props(Props[Slave]), "simplePoolMaster")

//  for (i <- 1 to 10) {
//    poolMaster ! s"hello from world $i"
//  }

  val poolMaster2 = system.actorOf(FromConfig.props(Props[Slave]), "poolMaster2")

//  for (i <- 1 to 10) {
//    poolMaster2 ! s"hello from world $i"
//  }

  val slaves: List[ActorRef] = (1 to 5).map(i => system.actorOf(Props[Slave], s"slave-$i")).toList
  val slavePaths = slaves.map(_.path.toString)
  val groupMaster = system.actorOf(RoundRobinGroup(slavePaths).props())

//  for (i <- 1 to 10) {
//    groupMaster ! s"hello from world $i"
//  }

  val groupMaster2 = system.actorOf(FromConfig.props(), "groupMaster2")

  for (i <- 1 to 10) {
    groupMaster2 ! s"hello from world $i"
  }
}
