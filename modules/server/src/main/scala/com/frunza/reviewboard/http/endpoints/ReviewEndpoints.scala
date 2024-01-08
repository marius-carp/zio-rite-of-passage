package com.frunza.reviewboard.http.endpoints

import sttp.tapir.*
import sttp.tapir.json.zio.jsonBody
import com.frunza.reviewboard.http.requests.CreateReviewRequest
import com.frunza.reviewboard.domain.data.Review
import sttp.tapir.generic.auto._

trait ReviewEndpoints {

  val createEndpoint: Endpoint[Unit, CreateReviewRequest, Unit, Review, Any] = endpoint
      .tag("Reviews")
      .name("create")
      .description("create a review for a company")
      .in("reviews")
      .post
      .in(jsonBody[CreateReviewRequest])
      .out(jsonBody[Review])


    val getByIdEndpoint: Endpoint[Unit, Long, Unit, Option[Review], Any] = endpoint
        .tag("Reviews")
        .name("getById")
        .description("Get a review by its id")
        .in("reviews" / path[Long]("id"))
        .get
        .out(jsonBody[Option[Review]])

    val getByCompanyIdEndpoint: Endpoint[Unit, Long, Unit, List[Review], Any] = endpoint
        .tag("Reviews")
        .name("getByCompanyId")
        .description("Get a reviews for a company")
        .in("reviews" / "company" / path[Long]("id"))
        .get
        .out(jsonBody[List[Review]])

}
