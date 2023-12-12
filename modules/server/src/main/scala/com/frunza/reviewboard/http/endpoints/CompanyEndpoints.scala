package com.frunza.reviewboard.http.endpoints

import sttp.tapir.*
import sttp.tapir.json.zio.jsonBody
import com.frunza.reviewboard.domain.data.Company
import com.frunza.reviewboard.http.requests.CreateCompanyRequest
import sttp.tapir.generic.auto._

trait CompanyEndpoints {

  val createEndpoint =
    endpoint
      .tag("companies")
      .name("create")
      .description("create a listing for a company")
      .in("companies")
      .post
      .in(jsonBody[CreateCompanyRequest])
      .out(jsonBody[Company])
    
  val getAllEndpoint =
    endpoint
      .tag("companies")
      .name("getAll")
      .description("get all companies listings")
      .in("companies")
      .get
      .out(jsonBody[List[Company]])
    
  val getByIdEndpoint =
    endpoint
      .tag("companies")
      .name("getById")
      .description("get company by its id")
      .in("companies" / path[String]("id"))
      .get
      .out(jsonBody[Option[Company]])

}
