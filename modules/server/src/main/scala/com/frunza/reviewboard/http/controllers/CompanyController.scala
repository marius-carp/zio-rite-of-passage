package com.frunza.reviewboard.http.controllers

import com.frunza.reviewboard.domain.data.Company
import com.frunza.reviewboard.http.endpoints.CompanyEndpoints
import com.frunza.reviewboard.services.CompanyService
import sttp.tapir.server.ServerEndpoint
import zio.{Task, ZIO}

import scala.collection.mutable

class CompanyController private(service: CompanyService) extends BaseController with CompanyEndpoints {
  
  val create: ServerEndpoint[Any, Task] = createEndpoint.serverLogicSuccess { req =>
    service.create(req)
  }

  val getAll: ServerEndpoint[Any, Task] =
    getAllEndpoint.serverLogicSuccess(_ => service.getAll)

  val getById: ServerEndpoint[Any, Task] =
    getByIdEndpoint.serverLogicSuccess { id =>
      ZIO.attempt(id.toLong)
        .flatMap(service.getById)
        .catchSome {
          case _: NumberFormatException =>
            service.getBySlug(id)
        }
    }

  override val routes: List[ServerEndpoint[Any, Task]] = List(create, getAll, getById)
}

object CompanyController {
  val makeZIO: ZIO[CompanyService, Nothing, CompanyController] = for {
    service <- ZIO.service[CompanyService]
  } yield new CompanyController(service)
}
