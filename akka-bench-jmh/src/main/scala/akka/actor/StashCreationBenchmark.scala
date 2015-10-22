/**
 * Copyright (C) 2015 Typesafe Inc. <http://www.typesafe.com>
 */
package akka.actor

import scala.concurrent.Await
import scala.concurrent.duration._
import akka.testkit.TestActors
import org.openjdk.jmh.annotations._
import java.util.concurrent.TimeUnit
import java.util.concurrent.CountDownLatch
import com.typesafe.config.ConfigFactory

object StashCreationBenchmark {
  class StashingActor(latch: CountDownLatch) extends Actor with Stash {

    override def preStart(): Unit = {
      latch.countDown()
    }

    def receive = {
      case msg => sender() ! msg
    }
  }

  def props(latch: CountDownLatch) = Props(new StashingActor(latch))
}

@State(Scope.Benchmark)
@BenchmarkMode(Array(Mode.SampleTime))
@Fork(3)
@Warmup(iterations = 5)
@Measurement(iterations = 10)
class StashCreationBenchmark {
  val conf = ConfigFactory.parseString("""
    my-dispatcher = {
      stash-capacity = 1000
    }
    """)
  implicit val system: ActorSystem = ActorSystem("StashCreationBenchmark", conf)

  @TearDown(Level.Trial)
  def shutdown() {
    system.terminate()
    Await.ready(system.whenTerminated, 15.seconds)
  }

  @Benchmark
  @OutputTimeUnit(TimeUnit.MICROSECONDS)
  def testDefault: Boolean = {
    val latch = new CountDownLatch(1)
    val stash = system.actorOf(StashCreationBenchmark.props(latch))
    latch.await()
    true
  }

  @Benchmark
  @OutputTimeUnit(TimeUnit.MICROSECONDS)
  def testCustom: Boolean = {
    val latch = new CountDownLatch(1)
    val stash = system.actorOf(StashCreationBenchmark.props(latch).withDispatcher("my-dispatcher"))
    latch.await()
    true
  }
}

