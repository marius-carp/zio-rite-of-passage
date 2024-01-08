package scala.com.frunza.reviewboard.repositories

import com.frunza.reviewboard.domain.data.Review
import com.frunza.reviewboard.repositories.{Repository, ReviewRepository, ReviewRepositoryLive}
import zio.test.*
import zio.*

import java.time.Instant
import scala.com.frunza.reviewboard.repositories.CompanyRepositorySpec.dataSourceLayer
import scala.com.frunza.reviewboard.syntax.*

object ReviewRepositorySpec extends ZIOSpecDefault with RepositorySpec {

  val goodReview = Review(
    id = 1L, companyId = 1L, userId = 1L, management = 5, culture = 5, salary = 5, benefits = 5, wouldRecommend = 10, review = "all good", created = Instant.now, updated = Instant.now
  )

  val badReview = Review(
    id = 1L, companyId = 1L, userId = 1L, management = 1, culture = 1, salary = 1, benefits = 1, wouldRecommend = 2, review = "all bad", created = Instant.now, updated = Instant.now
  )

  override val initScript: String = "sql/reviews.sql"

  override def spec: Spec[TestEnvironment with Scope, Any] =  {
    suite("ReviewRepositorySpec")(
      test("create review") {
        val program = for {
          repo <- ZIO.service[ReviewRepository]
          review <- repo.create(goodReview)
        } yield review

        program.assert { review =>
          review.management == goodReview.management &
          review.culture == goodReview.culture  &
          review.salary == goodReview.salary  &
          review.benefits == goodReview.benefits  &
          review.wouldRecommend == goodReview.wouldRecommend &
          review.review == goodReview.review
        }
      },
      test("get review by ids (id, companyId, userId)") {
        for {
          repo <- ZIO.service[ReviewRepository]
          review <- repo.create(goodReview)
          fetchedReview <- repo.getById(review.id)
          fetchedReview2 <- repo.getByCompanyId(review.companyId)
          fetchedReview3 <- repo.getByUserId(review.userId)
        } yield assertTrue (
          fetchedReview.contains(review) &&
            fetchedReview2.contains(review) &&
            fetchedReview3.contains(review)
        )
      },
      test("get all") {
        for {
          repo <- ZIO.service[ReviewRepository]
          review1 <- repo.create(goodReview)
          review2 <- repo.create(badReview)
          reviewsCompany <- repo.getByCompanyId(1L)
          reviewsUser <- repo.getByUserId(1L)
        } yield assertTrue (
          reviewsCompany.toSet == Set(review1, review2) &&
            reviewsUser.toSet == Set(review1, review2)
        )
      },
      test("edit review") {
        for {
          repo <- ZIO.service[ReviewRepository]
          review <- repo.create(goodReview)
          updated <- repo.update(review.id, _.copy(review = "not too bad"))
        } yield assertTrue (
          review.id == updated.id &&
          review.companyId == updated.companyId &&
          review.userId == updated.userId &&
          review.management == updated.management &&
          review.culture == updated.culture &&
          review.salary == updated.salary &&
          review.benefits == updated.benefits &&
          review.wouldRecommend == updated.wouldRecommend &&
          updated.review == "not too bad" &&
          review.updated != updated.updated
        )
      },
      test("delete review") {
        for {
          repo <- ZIO.service[ReviewRepository]
          review <- repo.create(goodReview)
          _ <- repo.delete(review.id)
          maybeReview <- repo.getById(review.id)
        } yield assertTrue(maybeReview.isEmpty)
      }
    ).provide(
      ReviewRepositoryLive.layer,
      dataSourceLayer,
      Repository.quillLayer,
      Scope.default
    )
  }
}
