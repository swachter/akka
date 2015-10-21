/**
 * Copyright (C) 2015 Typesafe Inc. <http://www.typesafe.com>
 */
package akka.actor

import scala.concurrent.Await
import scala.concurrent.duration._

import akka.testkit.TestActors
import akka.testkit.TestProbe
import org.openjdk.jmh.annotations._
import java.util.concurrent.TimeUnit

object StashCreationBenchmark {
  class StashingActor extends Actor with Stash {
    def receive = {
      case msg => sender() ! msg
    }
  }

  val props = Props[StashingActor]
}

@State(Scope.Benchmark)
@BenchmarkMode(Array(Mode.SampleTime))
@Fork(3)
@Warmup(iterations = 5)
@Measurement(iterations = 10)
class StashCreationBenchmark {
  implicit val system: ActorSystem = ActorSystem()
  val probe = TestProbe()

  Props[TestActors.EchoActor]

  @TearDown(Level.Trial)
  def shutdown() {
    system.terminate()
    Await.ready(system.whenTerminated, 15.seconds)
  }

  @Benchmark
  @OutputTimeUnit(TimeUnit.MICROSECONDS)
  def testCreation: Boolean = {
    val stash = system.actorOf(StashCreationBenchmark.props)
    stash.tell("hello", probe.ref)
    probe.expectMsg("hello")
    true
  }
}

