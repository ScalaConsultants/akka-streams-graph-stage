package io.scalac.streams.stage.proxy

import akka.stream.stage.{GraphStage, GraphStageLogic, InHandler, OutHandler}
import akka.stream.{Attributes, BidiShape, Inlet, Outlet}
import akka.util.ByteString

class NeutralGraphStage extends GraphStage[BidiShape[ByteString, ByteString, ByteString, ByteString]] {
  val bytesIn: Inlet[ByteString] = Inlet("OutgoingTCP.in")
  val bytesOut: Outlet[ByteString] = Outlet("OutgoingTCP.out")

  val sslIn: Inlet[ByteString] = Inlet("OutgoingSSL.in")
  val sslOut: Outlet[ByteString] = Outlet("OutgoingSSL.out")

  override def shape: BidiShape[ByteString, ByteString, ByteString, ByteString] = BidiShape.apply(sslIn, bytesOut, bytesIn, sslOut)

  override def createLogic(inheritedAttributes: Attributes): GraphStageLogic = new GraphStageLogic(shape) {
    setHandler(sslIn, new InHandler {
      override def onPush() = push(bytesOut, grab(sslIn))
    })

    setHandler(bytesIn, new InHandler {
      override def onPush() = push(sslOut, grab(bytesIn))
    })

    setHandler(bytesOut, new OutHandler {
      override def onPull() = pull(sslIn)
    })

    setHandler(sslOut, new OutHandler {
      override def onPull() = pull(bytesIn)
    })

  }
}
