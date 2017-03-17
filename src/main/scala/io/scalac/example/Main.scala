package io.scalac.example

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{BidiFlow, Flow, Sink, Source}
import akka.util.ByteString
import io.scalac.example.stage.{MyFilterGraphStage, ProxyGraphStage, SugaredMyFilterGraphStage}

object Main {
  def main(args: Array[String]): Unit = {
    implicit val system = ActorSystem()
    implicit val mat = ActorMaterializer()
    implicit val ec = system.dispatcher

    val source = Source((1 to 10).toList)
    val myFilterStage = new MyFilterGraphStage[Int](_ % 2 == 0)
    val myFilter = Flow.fromGraph(myFilterStage)
    val done = source.via(myFilter).runForeach(println)

    done.foreach { _ =>
      mat.shutdown()
      system.terminate()
    }
  }
}

object Main2 {
  def main(args: Array[String]): Unit = {
    implicit val system = ActorSystem()
    implicit val mat = ActorMaterializer()
    implicit val ec = system.dispatcher

    val source = Source((1 to 10).toList)
    val myFilterStage = new SugaredMyFilterGraphStage[Int](_ % 2 == 0)
    val myFilter = Flow.fromGraph(myFilterStage)
    val done = source.via(myFilter).runForeach(println)

    done.foreach { _ =>
      mat.shutdown()
      system.terminate()
    }
  }
}

object Main3 {
  def main(args: Array[String]): Unit = {
    implicit val system = ActorSystem()
    implicit val mat = ActorMaterializer()
    implicit val ec = system.dispatcher

    val source = Source(List(ByteString("abc"), ByteString("def")))
    val proxyGraphStage = new ProxyGraphStage("hostname", 8888)
    val proxyFlow = BidiFlow.fromGraph(proxyGraphStage)

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
    val done = source.via(wholeFlow).runForeach(element => println("Sink received: " + element.utf8String))

    done.onComplete {
      case res =>
        println("res: " + res)
        mat.shutdown()
        system.terminate()
    }
  }
}
