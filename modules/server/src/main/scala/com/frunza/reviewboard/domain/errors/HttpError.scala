package com.frunza.reviewboard.domain.errors

import sttp.model.StatusCode

final case class HttpError(
    statusCode: StatusCode,
    message: String,
    cause: Throwable) extends RuntimeException(message, cause)

object HttpError {
  def decode(tuple: (StatusCode, String)): HttpError =
    HttpError(tuple._1, tuple._2, new RuntimeException(tuple._2))

  //TODO add some more cases and return other codes
  def encode(error: Throwable): (StatusCode, String) = error match {
    case UnauthorizedException => (StatusCode.Unauthorized, error.getMessage)
    case _ => (StatusCode.InternalServerError, error.getMessage)
  }
}