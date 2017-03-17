package io.scalac.example

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Flow, Source}
import io.scalac.example.stage.MyFilterGraphStage

object Main {
  def main(args: Array[String]): Unit = {
    implicit val system = ActorSystem()
    implicit val mat = ActorMaterializer()
    implicit val ec = system.dispatcher

    println("Hello, world!")

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
