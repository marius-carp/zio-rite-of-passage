package com.frunza.reviewboard.http.controllers

import com.frunza.reviewboard.domain.data.Company
import com.frunza.reviewboard.http.endpoints.CompanyEndpoints
import sttp.tapir.server.ServerEndpoint
import zio.{Task, ZIO}

import scala.collection.mutable

class CompanyController extends BaseController with CompanyEndpoints {

  // TODO implementations
  // in-memory db
  val db = mutable.Map[Long, Company]()

  val create: ServerEndpoint[Any, Task] = createEndpoint.serverLogicSuccess { req =>
    ZIO.succeed {
      val newId = db.keys.maxOption.getOrElse(0L) + 1L
      val newCompany = req.toCompany(newId)
      db += newId -> newCompany

      newCompany
    }
  }

  val getAll: ServerEndpoint[Any, Task] =
    getAllEndpoint.serverLogicSuccess(_ => ZIO.succeed(db.values.toList))

  val getById: ServerEndpoint[Any, Task] =
    getByIdEndpoint.serverLogicSuccess { id =>
      ZIO.attempt(id.toLong)
        .map(db.get)
    }

  override val routes: List[ServerEndpoint[Any, Task]] = List(create, getAll, getById)
}

object CompanyController {
  val makeZio = ZIO.succeed(new CompanyController)
}
