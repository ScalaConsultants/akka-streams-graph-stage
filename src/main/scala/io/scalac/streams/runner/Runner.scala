package io.scalac.streams.runner

import akka.actor.ActorSystem
import akka.stream.{ActorMaterializer, Materializer}

import scala.concurrent.Future

trait Runner {
  def run[T](fun: (Materializer) => Future[T]): Unit = {
    implicit val system = ActorSystem()
    val mat = Materializer.createMaterializer(system)
    implicit val ec = system.dispatcher

    fun(mat).onComplete {
      case res =>
        println("res: " + res)
        mat.shutdown()
        system.terminate()
    }
  }
}
