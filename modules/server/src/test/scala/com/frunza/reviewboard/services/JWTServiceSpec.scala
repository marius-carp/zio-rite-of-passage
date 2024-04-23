package com.frunza.reviewboard.services

import com.frunza.reviewboard.config.JWTConfig
import com.frunza.reviewboard.domain.data.User
import zio.*
import zio.test.*

object JWTServiceSpec extends ZIOSpecDefault {

  override def spec: Spec[TestEnvironment & Scope, Any] =
    suite("JWTServiceSpec")(
      test("create and validate token") {
        for {
          service <- ZIO.service[JWTService]
          userToken <- service.createToken(User(1L, "marius@frunza.com", "what"))
          user <- service.verifyToken(userToken.token)
        } yield assertTrue(
          user.id == 1L &&
          user.email == "marius@frunza.com"
        )
      }
    ).provide(
      JWTServiceLive.layer,
      ZLayer.succeed(JWTConfig("secret", 3600))
    )
}
