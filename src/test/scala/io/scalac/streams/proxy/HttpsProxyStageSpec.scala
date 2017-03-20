package io.scalac.streams.proxy

import akka.actor.ActorSystem
import akka.stream.scaladsl.{BidiFlow, Flow, Keep, Sink, Source}
import akka.stream.testkit.scaladsl.{TestSink, TestSource}
import akka.stream.testkit.{TestPublisher, TestSubscriber}
import akka.stream.{ActorMaterializer, ActorMaterializerSettings}
import akka.testkit.TestKit
import akka.util.ByteString
import io.scalac.streams.stage.proxy._
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.exceptions.TestFailedException
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpecLike}

class HttpsProxyStageSpec (_system: ActorSystem) extends TestKit(_system) with WordSpecLike with Matchers with BeforeAndAfterAll
    with ScalaFutures {

  implicit val materializer = ActorMaterializer(ActorMaterializerSettings(_system).withFuzzing(true))

  def this() = this(ActorSystem())

  "A ProxyGraphStage" should {
    "treat any 2xx response for CONNECT message as successful" in new Context {
      source.sendNext(ByteString("anything"))
      sink.request(1)

      transportInProbe.requestNext()
      transportOutProbe.sendNext(ByteString("OK"))

      val recv = transportInProbe.requestNext()
      transportOutProbe.sendNext(recv)
      sink.expectNext(ByteString("anything"))
      source.sendComplete()
      transportOutProbe.sendComplete()
      sink.expectComplete()
    }

    "should handle fast demand from flowInProbe" in new Context {
      source.sendNext(ByteString("anything"))
      sink.request(1)

      transportInProbe.requestNext()
      transportInProbe.request(1)
      transportOutProbe.sendNext(ByteString("OK"))

      val recv = transportInProbe.expectNext()
      transportOutProbe.sendNext(recv)
      sink.expectNext(ByteString("anything"))
      source.sendComplete()
      transportOutProbe.sendComplete()
      sink.expectComplete()
    }

    "should be transparent for errors" in new Context {
//      val (specificSource, done) = TestSource.probe[ByteString]
//        .via(flowUnderTest)
//        .toMat(Sink.head)(Keep.both)
//        .run()

      source.sendNext(ByteString("anything"))
      sink.request(1)

      transportInProbe.cancel()
      transportOutProbe.sendError(new TestException("abc"))

      source.expectCancellation()
      sink.expectError(new TestException("abc"))

//      transportInProbe.cancel()
//      println("HERE!!")
//      println("HERE!! 2")
//      transportOutProbe.sendError(new RuntimeException("abc"))
//      val exception = intercept[TestFailedException](done.futureValue)
//      println("bazinga: " + exception)
//      println("bazinga: " + exception.cause)
//      println("bazinga: " + exception.cause.get.getMessage)
//      exception.cause.get.getMessage should equal("abc")
    }

  }

  override def afterAll(): Unit = {
    materializer.shutdown()
    _system.terminate()
  }

  trait Context {
    val targetHostName = "akka.io"
    val targetPort = 443

    // Here you can instantiate different implementation - e.g. some fault one like HttpsProxyGraphStage0
    val proxyGraphStage = new CorrectHttpsProxyGraphStage(targetHostName, targetPort)
    val proxyGraph = BidiFlow.fromGraph(proxyGraphStage)

    val transportInProbe = TestSubscriber.probe[ByteString]()
    val transportOutProbe = TestPublisher.probe[ByteString]()

    val transportFlow = Flow.fromSinkAndSource(
      Sink.fromSubscriber(transportInProbe),
      Source.fromPublisher(transportOutProbe))

    val flowUnderTest = proxyGraph.join(transportFlow)

    val (source, sink) = TestSource.probe[ByteString]
      .via(flowUnderTest)
      .toMat(TestSink.probe)(Keep.both)
      .run()
  }

  case class TestException(msg: String) extends Exception
}
