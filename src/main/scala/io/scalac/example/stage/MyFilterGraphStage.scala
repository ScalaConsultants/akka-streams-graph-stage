package io.scalac.example.stage

import akka.stream.{Attributes, FlowShape, Inlet, Outlet}
import akka.stream.stage.{GraphStage, GraphStageLogic, InHandler, OutHandler}

class MyFilterGraphStage[T](predicate: T => Boolean) extends GraphStage[FlowShape[T, T]] {
  // inlets/outlets names ("MyFilter.in" and "MyFilter.out" in this case)
  // serves mostly for internal diagnostic messages in case of failures
  val input = Inlet[T]("MyFilter.in")
  val output = Outlet[T]("MyFilter.out")

  override def shape: FlowShape[T, T] = FlowShape(input, output)

  override def createLogic(inheritedAttributes: Attributes): GraphStageLogic = new GraphStageLogic(shape) {
    setHandler(input, new InHandler {
      override def onPush(): Unit = {
        val element = grab(input)
        if(predicate(element)) {
          push(output, element)
        } else {
          pull(input)
        }
      }
    })

    setHandler(output, new OutHandler {
      override def onPull() = pull(input)
    })
  }

}
