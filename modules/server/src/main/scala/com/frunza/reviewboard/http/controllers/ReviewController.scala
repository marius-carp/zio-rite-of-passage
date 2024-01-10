package com.frunza.reviewboard.http.controllers

import com.frunza.reviewboard.http.endpoints.ReviewEndpoints
import com.frunza.reviewboard.services.ReviewService
import sttp.tapir.server.ServerEndpoint
import zio.*

class ReviewController private (reviewService: ReviewService) extends  BaseController with ReviewEndpoints {

  val create: ServerEndpoint[Any, Task] = createEndpoint.serverLogic(req => reviewService.create(req, 1L).either)

  val getById: ServerEndpoint[Any, Task] = getByIdEndpoint.serverLogic(id => reviewService.getById(id).either)

  val getByCompanyId: ServerEndpoint[Any, Task] = getByCompanyIdEndpoint.serverLogic(companyId => reviewService.getByCompanyId(companyId).either)

  override val routes: List[ServerEndpoint[Any, Task]] = List(
    create,
    getById,
    getByCompanyId
  )

}

object ReviewController {
  val makeZIO = ZIO.service[ReviewService].map(reviewService => new ReviewController(reviewService))
}
