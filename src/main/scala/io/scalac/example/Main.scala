package io.scalac.example

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{BidiFlow, Flow, Sink, Source}
import akka.util.ByteString
import io.scalac.example.stage.proxy.{NeutralGraphStage, ProxyGraphStage, ProxyGraphStage0}
import io.scalac.example.stage.{MyFilterGraphStage, SugaredMyFilterGraphStage}

import scala.concurrent.{ExecutionContext, Future}

object Main extends Runner {
  def main(args: Array[String]): Unit = {
    run { implicit mat =>
      val source = Source((1 to 10).toList)
      val myFilterStage = new MyFilterGraphStage[Int](_ % 2 == 0)
      val myFilter = Flow.fromGraph(myFilterStage)
      source.via(myFilter).runForeach(println)
    }
  }
}

object Main2 extends Runner {
  def main(args: Array[String]): Unit = {
    run { implicit mat =>
      val source = Source((1 to 10).toList)
      val myFilterStage = new SugaredMyFilterGraphStage[Int](_ % 2 == 0)
      val myFilter = Flow.fromGraph(myFilterStage)
      source.via(myFilter).runForeach(println)
    }
  }
}

object Main3 extends Runner {
  def main(args: Array[String]): Unit = {
    run { implicit mat =>
      val source = Source(List(ByteString("abc"), ByteString("def")))
      val proxyGraphStage = new ProxyGraphStage0("hostname", 8888)
      val proxyFlow = BidiFlow.fromGraph(proxyGraphStage)

      val flow = Flow[ByteString].statefulMapConcat { () =>
        var firstElement = true

        (input: ByteString) => {
          if(firstElement) {
            firstElement = false
            List(ByteString("OKff"))
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

object RunNeutral extends Runner {
  def main(args: Array[String]): Unit = {
    run { implicit mat =>
      val source = Source(List(ByteString("abc"), ByteString("def")))
      val proxyGraphStage = new NeutralGraphStage
      val proxyFlow = BidiFlow.fromGraph(proxyGraphStage)
      val transport = Flow[ByteString].map(bs => bs)

      source.via(proxyFlow.join(transport)).runForeach(element => println("Sink received: " + element.utf8String))
    }
  }
}

trait Runner {
  def run[T](fun: (ActorMaterializer) => Future[T]): Unit = {
    implicit val system = ActorSystem()
    val mat = ActorMaterializer()
    implicit val ec = system.dispatcher

    fun(mat).onComplete {
      case res =>
        println("res: " + res)
        mat.shutdown()
        system.terminate()
    }
  }
}
