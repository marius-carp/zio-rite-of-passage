package com.frunza.reviewboard.http.endpoints

import com.frunza.reviewboard.domain.errors.HttpError
import sttp.tapir.*

trait BaseEndpoint {

  val baseEndpoint = endpoint
    .errorOut(statusCode and plainBody[String])
    .mapErrorOut[Throwable](HttpError.decode)(HttpError.encode)

  val secureBaseEndpoint =
    baseEndpoint
      .securityIn(auth.bearer[String]())
}
