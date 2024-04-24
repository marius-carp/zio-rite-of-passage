package com.frunza.reviewboard

import com.frunza.reviewboard.config.{Configs, JWTConfig}
import com.frunza.reviewboard.http.HttpApi
import com.frunza.reviewboard.repositories.{CompanyRepositoryLive, Repository, ReviewRepositoryLive, UserRepositoryLive}
import com.frunza.reviewboard.services.{CompanyServiceLive, JWTServiceLive, ReviewServiceLive, UserServiceLive}
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
      //conf
      Configs.makeLayer[JWTConfig]("frunza.jwt"),
      // repos 
      CompanyRepositoryLive.layer,
      ReviewRepositoryLive.layer,
      UserRepositoryLive.layer,
      //services
      CompanyServiceLive.layer,
      ReviewServiceLive.layer,
      UserServiceLive.layer,
      JWTServiceLive.layer,
      // other
      Repository.dataLayer
    )

}
