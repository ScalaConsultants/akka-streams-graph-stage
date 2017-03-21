package io.scalac.streams.runner

import akka.stream.scaladsl.{BidiFlow, Flow, Source}
import akka.util.ByteString
import io.scalac.streams.stage.proxy.CorrectHttpsProxyGraphStage

class ProxyStageRunner extends Runner {
  def main(args: Array[String]): Unit = {
    run { implicit mat =>
      val source = Source(List(ByteString("abc"), ByteString("def")))
      val proxyGraphStage = new CorrectHttpsProxyGraphStage("hostname", 8888)
      val proxyFlow = BidiFlow.fromGraph(proxyGraphStage)

      val flow = Flow[ByteString].statefulMapConcat { () =>
        var firstElement = true

        (input: ByteString) => {
          if(firstElement) {
            firstElement = false
            List(ByteString("OK ff"))
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
