package io.scalac.streams.runner

import akka.stream.scaladsl.{BidiFlow, Flow, Source}
import akka.util.ByteString
import io.scalac.streams.stage.proxy._

class ProxyStageRunner extends Runner {
  def main(args: Array[String]): Unit = {
    run { implicit mat =>
      val source = Source(List(ByteString("abc"), ByteString("def")))

      // INFO:
      // You can change it to any earlier implementation e.g. HttpsProxyStage1
      val proxyStage = new HttpsProxyStage0("hostname", 8888)

      val proxyFlow = BidiFlow.fromGraph(proxyStage)

      val flow = Flow[ByteString].statefulMapConcat { () =>
        var firstElement = true

        (input: ByteString) => {
          if(firstElement) {
            firstElement = false
            List(ByteString("OK"))
          } else {
            List(input)
          }
        }
      }

      val wholeFlow = proxyFlow.join(flow)
      source.via(wholeFlow).runForeach(element => println("Sink received: " + element.utf8String))
    }
  }
}
