package io.scalac.streams.stage.basic

import akka.event.Logging
import akka.stream.{Attributes, FlowShape, Inlet, Outlet}
import akka.stream.stage.{GraphStage, GraphStageLogic, InHandler, OutHandler}

class SugaredMyFilterGraphStage[T](predicate: T => Boolean) extends GraphStage[FlowShape[T, T]] {
  val in = Inlet[T](Logging.simpleName(this) + ".in")
  val out = Outlet[T](Logging.simpleName(this) + ".out")
  override val shape = FlowShape(in, out)

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
