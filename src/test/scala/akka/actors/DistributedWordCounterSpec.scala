package akka.actors

import akka.testkit.{EventFilter, TestProbe}

class DistributedWordCounterSpec extends AkkaSpec("DistributedWordCounterSpec") {

  "A DistributedWordCounter actor" should {


    "log warning when not initialised" in {
      EventFilter.warning("unable to perform any operations until initialised", occurrences = 1).intercept {
        val master = system.actorOf(DistributedWordCounter.props)

        master ! DistributedWordCounter.WordCountRequest("hello, world!")
      }
    }

    "initialise with n amount of workers and log message" in {
      EventFilter.info("initialising DistributedWordCounter with 10 workers", occurrences = 1).intercept {
        val master = system.actorOf(DistributedWordCounter.props)

        master ! DistributedWordCounter.Initialise(10)
      }
    }

    "register a worker and send work to it" in {
      val master = system.actorOf(DistributedWordCounter.props)
      val worker = TestProbe("word-count-worker")

      master ! DistributedWordCounter.Register(List(worker.ref))
      master ! DistributedWordCounter.WordCountRequest("hello, world!")

      worker.receiveWhile() {
        case WordCountWorker.WorkerRequest(0, "hello, world!") => worker.reply(WordCountWorker.WorkerResponse(0, 2))
      }

      expectMsg(DistributedWordCounter.WordCountResponse(0))

      master ! DistributedWordCounter.WordCountEnquiryRequest(0)
      expectMsg(DistributedWordCounter.WordCountEnquiryResponse(0, Some(2)))
    }

    "return none if job does not exist" in {
      val master = system.actorOf(DistributedWordCounter.props)
      val worker = TestProbe("word-count-worker")

      master ! DistributedWordCounter.Register(List(worker.ref))

      master ! DistributedWordCounter.WordCountEnquiryRequest(0)
      expectMsg(DistributedWordCounter.WordCountEnquiryResponse(0, None))
    }

    "alternate work between workers" in {
      val master = system.actorOf(DistributedWordCounter.props)
      val worker1 = TestProbe("word-count-worker-1")
      val worker2 = TestProbe("word-count-worker-2")

      master ! DistributedWordCounter.Register(List(worker1.ref, worker2.ref))

      master ! DistributedWordCounter.WordCountRequest("hello, world!")
      master ! DistributedWordCounter.WordCountRequest("hello, world again!")


      worker1.expectMsg(WordCountWorker.WorkerRequest(0, "hello, world!"))
      worker1.reply(WordCountWorker.WorkerResponse(0, 2))

      worker2.expectMsg(WordCountWorker.WorkerRequest(1, "hello, world again!"))
      worker2.reply(WordCountWorker.WorkerResponse(1, 3))

      expectMsgAllOf(
        DistributedWordCounter.WordCountResponse(0),
        DistributedWordCounter.WordCountResponse(1)
      )

      master ! DistributedWordCounter.WordCountEnquiryRequest(1)
      expectMsg(DistributedWordCounter.WordCountEnquiryResponse(1, Some(3)))
    }
  }
}
