package io.scalac.streams.stage.proxy.common

object HttpsProxyState {
  sealed trait State

  // Entry state
  case object Starting extends State

  // State after CONNECT messages has been sent to Proxy and before Proxy responded back
  case object Connecting extends State

  // State after Proxy responded back
  case object Connected extends State
}
