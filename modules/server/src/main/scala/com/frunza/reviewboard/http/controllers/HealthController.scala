package com.frunza.reviewboard.http.controllers

import com.frunza.reviewboard.http.endpoints.HealthEndpoint
import sttp.tapir.server.ServerEndpoint
import sttp.tapir.*
import zio.{Task, ZIO}

class HealthController private extends BaseController with HealthEndpoint {

  val health: ServerEndpoint[Any, Task] = healthEndpoint
    .serverLogicSuccess[Task](_ => ZIO.succeed("All good"))

  val error: ServerEndpoint[Any, Task] = errorEndpoint
    .serverLogic[Task](_ => ZIO.fail(new RuntimeException("Boom!")).either)

  override val routes: List[ServerEndpoint[Any, Task]] = List(health, error)
}

object HealthController {
  val makeZIO = ZIO.succeed(new HealthController)

}
