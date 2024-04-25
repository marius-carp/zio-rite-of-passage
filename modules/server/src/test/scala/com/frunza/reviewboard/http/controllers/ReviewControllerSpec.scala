package com.frunza.reviewboard.http.controllers

import com.frunza.reviewboard.domain.data.Review
import com.frunza.reviewboard.http.controllers.ReviewController
import com.frunza.reviewboard.http.requests.CreateReviewRequest
import com.frunza.reviewboard.services.ReviewService
import sttp.client3.*
import sttp.client3.testing.SttpBackendStub
import sttp.monad.MonadError
import sttp.tapir.server.stub.TapirStubInterpreter
import sttp.tapir.ztapir.RIOMonadError
import zio.test.*
import zio.*
import zio.json.*
import sttp.tapir.server.ServerEndpoint

import scala.com.frunza.reviewboard.syntax.*
import java.time.Instant

object ReviewControllerSpec extends ZIOSpecDefault {

  private given zioMonadError: MonadError[Task] = new RIOMonadError[Any]

  val goodReview = Review(
    id = 1L, companyId = 1L, userId = 1L, management = 5, culture = 5, salary = 5, benefits = 5, wouldRecommend = 10, review = "all good", created = Instant.now, updated = Instant.now
  )

  private val serviceStub = new ReviewService {
    override def create(request: CreateReviewRequest, userId: Long): Task[Review] = ZIO.succeed(goodReview)

    override def getById(id: Long): Task[Option[Review]] = ZIO.succeed {
      if (id == 1L) Some(goodReview)
      else None
    }

    override def getByCompanyId(companyId: Long): Task[List[Review]] = ZIO.succeed {
      if (companyId == 1L) List(goodReview)
      else List()
    }

    override def getByUserId(userid: Long): Task[List[Review]] = ZIO.succeed {
      if (userid == 1L) List(goodReview)
      else List()
    }

    override def update(id: Long, op: Review => Review): Task[Review] = ZIO.succeed(goodReview)

    override def delete(id: Long): Task[Review] = ZIO.succeed(goodReview)
  }

  private def backendStubZIO(endpointFun: ReviewController => ServerEndpoint[Any, Task]) = for {
    controller <- ReviewController.makeZIO
    backendStub <- ZIO.succeed(TapirStubInterpreter(SttpBackendStub(MonadError[Task]))
      .whenServerEndpointRunLogic(endpointFun(controller))
      .backend()
    )
  } yield backendStub

  override def spec: Spec[TestEnvironment with Scope, Any] =
    suite("ReviewControllerSpec")(
      test("post review") {
        val program = for {
          backendStub <- backendStubZIO(_.create)
          response <- basicRequest
            .post(uri"reviews")
            .body(CreateReviewRequest(companyId = 1L, management = 5, culture = 5, salary = 5, benefits = 5, wouldRecommend = 10, review = "all good").toJson)
            .send(backendStub)
        } yield response.body

        program.assert(
          _.toOption
            .flatMap(_.fromJson[Review].toOption)
            .contains(goodReview)
        )
      },
      test("get by id") {
        for {
          backendStub <- backendStubZIO(_.getById)
          response <- basicRequest
            .get(uri"reviews/1")
            .send(backendStub)
          responseNotFound <- basicRequest
            .get(uri"reviews/999")
            .send(backendStub)
        } yield assertTrue (
          response.body.toOption.flatMap(_.fromJson[Review].toOption).contains(goodReview) &&
            responseNotFound.body.toOption.flatMap(_.fromJson[Review].toOption).isEmpty
        )
      },
      test("get by company id") {
        for {
          backendStub <- backendStubZIO(_.getByCompanyId)
          response <- basicRequest
            .get(uri"reviews/company/1")
            .send(backendStub)
          responseNotFound <- basicRequest
            .get(uri"reviews/company/999")
            .send(backendStub)
        } yield assertTrue (
          response.body.toOption.flatMap(_.fromJson[List[Review]].toOption).contains(List(goodReview)) &&
            responseNotFound.body.toOption.flatMap(_.fromJson[List[Review]].toOption).contains(List())
        )
      }
    ).provide(ZLayer.succeed(serviceStub))
}
