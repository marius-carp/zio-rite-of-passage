package com.frunza.reviewboard.services

import com.frunza.reviewboard.domain.data.Review
import com.frunza.reviewboard.http.requests.CreateReviewRequest
import com.frunza.reviewboard.repositories.ReviewRepository
import zio.test.*
import zio.*

import java.time.Instant

object ReviewServiceSpec extends ZIOSpecDefault {

  val goodReview = Review(
    id = 1L, companyId = 1L, userId = 1L, management = 5, culture = 5, salary = 5, benefits = 5, wouldRecommend = 10, review = "all good", created = Instant.now, updated = Instant.now
  )

  val badReview = Review(
    id = 1L, companyId = 1L, userId = 1L, management = 1, culture = 1, salary = 1, benefits = 1, wouldRecommend = 2, review = "all bad", created = Instant.now, updated = Instant.now
  )

  val stubRepoLayer = ZLayer.succeed {
    new ReviewRepository {
      override def create(review: Review): Task[Review] = ZIO.succeed(goodReview)

      override def getById(id: Long): Task[Option[Review]] = ZIO.succeed( {
        id match
          case 1 => Some(goodReview)
          case 2 => Some(badReview)
          case _ => None
      })

      override def getByCompanyId(id: Long): Task[List[Review]] = ZIO.succeed {
        if (id == 1) List(goodReview, badReview)
        else List()
      }

      override def getByUserId(userId: Long): Task[List[Review]] = ZIO.succeed {
        if (userId == 1) List(goodReview, badReview)
        else List()
      }

      override def update(id: Long, op: Review => Review): Task[Review] =
        getById(id).someOrFail(new RuntimeException(s"id not found: $id")).map(op)

      override def delete(id: Long): Task[Review] =
        getById(id).someOrFail(new RuntimeException(s"id not found: $id"))
    }
  }


  override def spec: Spec[TestEnvironment with Scope, Any] = {
    suite("ReviewServiceSpec")(
      test("create") {
        for {
          service <- ZIO.service[ReviewService]
          review <- service.create(
            CreateReviewRequest(
              companyId = 1L, management = 5, culture = 5, salary = 5, benefits = 5, wouldRecommend = 10, review = "all good"
            ),
            1L
          )
        } yield assertTrue(
          review.companyId == goodReview.companyId &&
            review.userId == goodReview.userId &&
            review.management == goodReview.management &&
            review.culture == goodReview.culture &&
            review.salary == goodReview.salary &&
            review.benefits == goodReview.benefits &&
            review.wouldRecommend == goodReview.wouldRecommend &&
            review.review == goodReview.review
        )
      },
      test("get by id") {
        for {
          service <- ZIO.service[ReviewService]
          review <- service.getById(1L)
          reviewNotFound <- service.getById(999L)
        } yield assertTrue(
          review.contains(goodReview) &&
            reviewNotFound.isEmpty
        )
      },
      test("get by company") {
        for {
          service <- ZIO.service[ReviewService]
          review <- service.getByCompanyId(1L)
          reviewNotFound <- service.getByCompanyId(999L)
        } yield assertTrue(
          review.toSet == Set(goodReview, badReview) &&
            reviewNotFound.isEmpty
        )
      },
      test("get by user") {
        for {
          service <- ZIO.service[ReviewService]
          review <- service.getByUserId(1L)
          reviewNotFound <- service.getByUserId(999L)
        } yield assertTrue(
          review.toSet == Set(goodReview, badReview) &&
            reviewNotFound.isEmpty
        )
      },
    ).provide(
      ReviewServiceLive.layer,
      stubRepoLayer
    )
  }
}
