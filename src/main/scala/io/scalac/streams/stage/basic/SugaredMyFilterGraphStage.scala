package io.scalac.streams.stage.basic

import akka.stream.Attributes
import akka.stream.impl.fusing.GraphStages.SimpleLinearGraphStage
import akka.stream.stage.{GraphStageLogic, InHandler, OutHandler}

class SugaredMyFilterGraphStage[T](predicate: T => Boolean) extends SimpleLinearGraphStage[T] {
  override def createLogic(inheritedAttributes: Attributes): GraphStageLogic = new GraphStageLogic(shape) with InHandler with OutHandler {
    override def onPush() = {
      val element = grab(in)
      if(predicate(element)) {
        push(out, element)
      } else {
        pull(in)
      }
    }

    override def onPull() = pull(in)

    setHandlers(in, out, this)
  }
}
