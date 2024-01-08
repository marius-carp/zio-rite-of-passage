package com.frunza.reviewboard.http.requests

import com.frunza.reviewboard.domain.data.Review
import zio.json.{DeriveJsonCodec, JsonCodec}

import java.time.Instant

case class CreateReviewRequest(companyId: Long,
                               userId: Long,
                               management: Int, // 1 - 5
                               culture: Int,
                               salary: Int,
                               benefits: Int,
                               wouldRecommend: Int,
                               review: String)

object CreateReviewRequest {

  def toReview(req: CreateReviewRequest): Review = {
    Review(
      id = -1L,
      companyId = req.companyId, userId = req.userId, management = req.management, culture = req.culture, salary = req.salary, benefits = req.benefits, wouldRecommend = req.wouldRecommend, review = req.review, created = Instant.now(), updated = Instant.now()
    )
  }

  given codec: JsonCodec[CreateReviewRequest] = DeriveJsonCodec.gen[CreateReviewRequest]
}