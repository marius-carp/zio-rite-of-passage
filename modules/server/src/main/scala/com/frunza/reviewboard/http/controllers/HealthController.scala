package com.frunza.reviewboard.http.controllers

import com.frunza.reviewboard.http.endpoints.HealthEndpoint
import zio.{Task, ZIO}

class HealthController private extends HealthEndpoint {

  val health = healthEndpoint
    .serverLogicSuccess[Task](_ => ZIO.succeed("All good"))

}

object HealthController {
  val makeZIO = ZIO.succeed(new HealthController)

}
