package io.scalac.streams.proxy

import akka.actor.ActorSystem
import akka.stream.scaladsl.{BidiFlow, Flow, Keep, Sink, Source}
import akka.stream.testkit.scaladsl.{TestSink, TestSource}
import akka.stream.testkit.{TestPublisher, TestSubscriber}
import akka.stream.{ActorMaterializer, ActorMaterializerSettings}
import akka.testkit.TestKit
import akka.util.ByteString
import io.scalac.streams.stage.proxy._
import io.scalac.streams.stage.proxy.common.ProxyConnectionFailedException
import org.scalatest.concurrent.ScalaFutures
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

    "should failStage for non-OK Proxy response" in new Context {
      source.sendNext(ByteString("anything"))
      sink.request(1)

      transportInProbe.requestNext()
      transportOutProbe.sendNext(ByteString("ERROR"))

      source.expectCancellation()
      sink.expectError(new ProxyConnectionFailedException(s"The HTTPS proxy rejected to open a connection to $targetHostName:$targetPort"))
    }

    "should be transparent for errors" in new Context {
      source.sendNext(ByteString("anything"))
      sink.request(1)

      transportInProbe.cancel()
      transportOutProbe.sendError(new TestException("abc"))

      source.expectCancellation()
      sink.expectError(new TestException("abc"))
    }

  }

  override def afterAll(): Unit = {
    materializer.shutdown()
    _system.terminate()
  }

  trait Context {
    val targetHostName = "akka.io"
    val targetPort = 443

    // Here you can instantiate different implementation - e.g. some fault one like HttpsProxyStage0
    val proxyStage = new CorrectHttpsProxyStage(targetHostName, targetPort)
    val proxyFlow = BidiFlow.fromGraph(proxyStage)

    val transportInProbe = TestSubscriber.probe[ByteString]()
    val transportOutProbe = TestPublisher.probe[ByteString]()

    val transportFlow = Flow.fromSinkAndSource(
      Sink.fromSubscriber(transportInProbe),
      Source.fromPublisher(transportOutProbe))

    val flowUnderTest = proxyFlow.join(transportFlow)

    val (source, sink) = TestSource.probe[ByteString]
      .via(flowUnderTest)
      .toMat(TestSink.probe)(Keep.both)
      .run()
  }

  case class TestException(msg: String) extends Exception
}
