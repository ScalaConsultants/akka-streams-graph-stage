package io.scalac.streams.stage.proxy

import akka.stream.stage.{GraphStage, GraphStageLogic, InHandler, OutHandler}
import akka.stream.{Attributes, BidiShape, Inlet, Outlet}
import akka.util.ByteString

class HttpsProxyStage0(targetHostName: String, targetPort: Int)
  extends GraphStage[BidiShape[ByteString, ByteString, ByteString, ByteString]] {

  import HttpsProxyState._

  val bytesIn: Inlet[ByteString] = Inlet("OutgoingTCP.in")
  val bytesOut: Outlet[ByteString] = Outlet("OutgoingTCP.out")

  val sslIn: Inlet[ByteString] = Inlet("OutgoingSSL.in")
  val sslOut: Outlet[ByteString] = Outlet("OutgoingSSL.out")

  override def shape: BidiShape[ByteString, ByteString, ByteString, ByteString] = BidiShape.apply(sslIn, bytesOut, bytesIn, sslOut)

  private val connectMsg = ByteString(s"CONNECT ${targetHostName}:${targetPort} HTTP/1.1\r\nHost: ${targetHostName}\r\n\r\n")

  override def createLogic(inheritedAttributes: Attributes): GraphStageLogic = new GraphStageLogic(shape) {
    private var state: State = Starting
    private var bufferedMsgs = Vector.empty[ByteString]

    setHandler(sslIn, new InHandler {
      override def onPush() = {
        state match {
          case Connected ⇒
            push(bytesOut, grab(sslIn))
          case _ ⇒
            bufferedMsgs :+= grab(sslIn)
        }
      }
    })

    setHandler(bytesIn, new InHandler {
      override def onPush() = {
        state match {
          case Starting ⇒
          // that means that proxy had sent us something even before CONNECT to proxy was sent, therefore we just ignore it
          case Connecting ⇒
            val proxyResponse = grab(bytesIn)
            if(proxyResponseValid(proxyResponse)) {
              state = Connected
              emitMultiple(bytesOut, bufferedMsgs)
            } else {
              failStage(new ProxyConnectionFailedException(s"The HTTPS proxy rejected to open a connection to $targetHostName:$targetPort"))
            }
          case Connected ⇒
            push(sslOut, grab(bytesIn))
        }
      }
    })

    setHandler(bytesOut, new OutHandler {
      override def onPull() = {
        state match {
          case Starting ⇒
            // We send connectMsg before any other message
            push(bytesOut, connectMsg)
            state = Connecting
            pull(sslIn)
          case Connecting ⇒
            pull(sslIn)
          case Connected ⇒
            pull(sslIn)
        }
      }
    })

    setHandler(sslOut, new OutHandler {
      override def onPull() = {
        pull(bytesIn)
      }
    })

    /**
      * Hugely simplified for sake of article.
      * We assume "OK" is the only valid response and that we will receive it as a single message.
      */
    private def proxyResponseValid(response: ByteString): Boolean =
      response.utf8String == "OK"
  }

}
