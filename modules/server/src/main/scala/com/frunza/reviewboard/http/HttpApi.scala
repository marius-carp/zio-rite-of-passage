package com.frunza.reviewboard.http

import com.frunza.reviewboard.http.controllers.{BaseController, CompanyController, HealthController, ReviewController, UserController}

object HttpApi {

  def gatherRoutes(controllers: List[BaseController]) =
    controllers.flatMap(_.routes)

  def makeController = for {
    health <- HealthController.makeZIO
    companies <- CompanyController.makeZIO
    reviews <- ReviewController.makeZIO
    users <- UserController.makeZIO
  } yield List(health, companies, reviews, users)

  val endpointsZIO = makeController.map(gatherRoutes)

}
