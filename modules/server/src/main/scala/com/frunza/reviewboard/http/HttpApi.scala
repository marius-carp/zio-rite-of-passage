package com.frunza.reviewboard.http

import com.frunza.reviewboard.http.controllers.{BaseController, CompanyController, HealthController}

object HttpApi {

  def gatherRoutes(controllers: List[BaseController]) =
    controllers.flatMap(_.routes)

  def makeController = for {
    health <- HealthController.makeZIO
    companies <- CompanyController.makeZio
  } yield List(health, companies)

  val endpointsZIO = makeController.map(gatherRoutes)

}
