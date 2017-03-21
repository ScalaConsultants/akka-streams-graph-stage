package io.scalac.streams.runner

import akka.stream.scaladsl.{Flow, Source}
import io.scalac.streams.stage.basic.MyFilterGraphStage

object BasicStageRunner extends Runner {
  def main(args: Array[String]): Unit = {
    run { implicit mat =>
      val source = Source((1 to 10).toList)
      val myFilterStage = new MyFilterGraphStage[Int](_ % 2 == 0)
      val myFilter = Flow.fromGraph(myFilterStage)
      source.via(myFilter).runForeach(println)
    }
  }
}
