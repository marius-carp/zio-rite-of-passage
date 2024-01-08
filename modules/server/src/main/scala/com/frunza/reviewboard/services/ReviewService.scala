package com.frunza.reviewboard.services

import com.frunza.reviewboard.domain.data.Review
import com.frunza.reviewboard.http.requests.CreateReviewRequest
import com.frunza.reviewboard.repositories.ReviewRepository
import zio.*

trait ReviewService {

  def create(request: CreateReviewRequest): Task[Review]
  def getById(id: Long): Task[Option[Review]]
  def getByCompanyId(companyId: Long): Task[List[Review]]
  def getByUserId(userid: Long): Task[List[Review]]
  def update(id: Long, op: Review => Review): Task[Review]
  def delete(id: Long): Task[Review]

}

class ReviewServiceLive private (repo: ReviewRepository) extends ReviewService {

  override def create(request: CreateReviewRequest): Task[Review] = repo.create(CreateReviewRequest.toReview(request))
  override def getById(id: Long): Task[Option[Review]] = repo.getById(id)
  override def getByCompanyId(companyId: Long): Task[List[Review]] = repo.getByCompanyId(companyId)
  override def getByUserId(userId: Long): Task[List[Review]] = repo.getByUserId(userId)
  override def update(id: Long, op: Review => Review): Task[Review] = repo.update(id, op)
  override def delete(id: Long): Task[Review] = repo.delete(id)

}

object ReviewServiceLive {
  val layer = ZLayer {
    ZIO.service[ReviewRepository].map(repo => new ReviewServiceLive(repo))
  }
}