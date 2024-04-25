package com.frunza.reviewboard

import com.frunza.reviewboard.config.{Configs, JWTConfig, RecoveryTokensConfig}
import com.frunza.reviewboard.http.HttpApi
import com.frunza.reviewboard.repositories.{CompanyRepositoryLive, RecoveryTokenRepositoryLive, Repository, ReviewRepositoryLive, UserRepositoryLive}
import com.frunza.reviewboard.services.{CompanyServiceLive, EmailServiceLive, JWTServiceLive, ReviewServiceLive, UserServiceLive}
import zio.*
import sttp.tapir.*
import sttp.tapir.server.ziohttp.{ZioHttpInterpreter, ZioHttpServerOptions}
import zio.http.Server

object Application extends ZIOAppDefault {

  val simpleProgram = for {
    endpoints <- HttpApi.endpointsZIO
    _ <- Server.serve(
      ZioHttpInterpreter(
        ZioHttpServerOptions.default
      ).toHttp(endpoints)
    )
    _ <- Console.printLine("Server started!")
  } yield()

  override def run =
    simpleProgram.provide(
      Server.default,
      // repos
      CompanyRepositoryLive.layer,
      ReviewRepositoryLive.layer,
      UserRepositoryLive.layer,
      RecoveryTokenRepositoryLive.configuredLayer,
      //services
      CompanyServiceLive.layer,
      ReviewServiceLive.layer,
      UserServiceLive.layer,
      JWTServiceLive.configuredLayer,
      EmailServiceLive.layer,
      // other
      Repository.dataLayer
    )

}
