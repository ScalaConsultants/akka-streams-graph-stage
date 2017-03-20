package io.scalac.streams.stage.proxy

import akka.stream.stage.{GraphStage, GraphStageLogic, InHandler, OutHandler}
import akka.stream.{Attributes, BidiShape, Inlet, Outlet}
import akka.util.ByteString

// This one will throw an exception
class CorrectHttpsProxyGraphStage(targetHostName: String, targetPort: Int)
  extends GraphStage[BidiShape[ByteString, ByteString, ByteString, ByteString]] {

    import HttpsProxyGraphStage._

    val bytesIn: Inlet[ByteString] = Inlet("OutgoingTCP.in")
    val bytesOut: Outlet[ByteString] = Outlet("OutgoingTCP.out")

    val sslIn: Inlet[ByteString] = Inlet("OutgoingSSL.in")
    val sslOut: Outlet[ByteString] = Outlet("OutgoingSSL.out")

    override def shape: BidiShape[ByteString, ByteString, ByteString, ByteString] = BidiShape.apply(sslIn, bytesOut, bytesIn, sslOut)

    private val connectMsg = ByteString(s"CONNECT ${targetHostName}:${targetPort} HTTP/1.1\r\nHost: ${targetHostName}\r\n\r\n")

    override def createLogic(inheritedAttributes: Attributes): GraphStageLogic = new GraphStageLogic(shape) {
      private var state: State = Starting

      setHandler(sslIn, new InHandler {
        override def onPush() = {
          state match {
            case Starting ⇒
              throw new IllegalStateException("inlet OutgoingSSL.in unexpectedly pushed in Starting state")
            case Connecting ⇒
              throw new IllegalStateException("inlet OutgoingSSL.in unexpectedly pushed in Connecting state")
            case Connected ⇒
              push(bytesOut, grab(sslIn))
          }
        }

        override def onUpstreamFinish(): Unit = complete(bytesOut)
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
                if(isAvailable(bytesOut)){
                  pull(sslIn)
                }
                pull(bytesIn)
              } else {
                failStage(new ProxyConnectionFailedException(s"The HTTPS proxy rejected to open a connection to $targetHostName:$targetPort"))
              }
            case Connected ⇒
              push(sslOut, grab(bytesIn))
          }
        }

        override def onUpstreamFinish(): Unit = complete(sslOut)
      })

      setHandler(bytesOut, new OutHandler {
        override def onPull() = {
          state match {
            case Starting ⇒
              push(bytesOut, connectMsg)
              state = Connecting
            case Connecting ⇒
            // don't need to do anything
            case Connected ⇒
              pull(sslIn)
          }
        }

        override def onDownstreamFinish(): Unit = cancel(sslIn)
      })

      setHandler(sslOut, new OutHandler {
        override def onPull() = {
          pull(bytesIn)
        }

        override def onDownstreamFinish(): Unit = cancel(bytesIn)
      })

      /**
        * Hugely simplified for sake of article.
        * We assume "OK" is the only valid response and that we will receive it as a single message.
        */
      private def proxyResponseValid(response: ByteString): Boolean =
        response.utf8String == "OK"
    }

  }
