package com.frunza.reviewboard.http.requests

import com.frunza.reviewboard.domain.data.Company
import zio.json.{DeriveJsonCodec, JsonCodec}

final case class CreateCompanyRequest(
  name: String,
  url: String,
  location: Option[String] = None,
  country: Option[String] = None,
  industry: Option[String] = None,
  image: Option[String] = None,
  tags: Option[List[String]] = None
) {
  def toCompany(id: Long): Company = {
    Company(
      id, Company.makeSlug(name), name, url, location, country, industry, image, tags.getOrElse(List.empty[String])
    )
  }
}

object CreateCompanyRequest {
  given codec: JsonCodec[CreateCompanyRequest] = DeriveJsonCodec.gen[CreateCompanyRequest]
}
