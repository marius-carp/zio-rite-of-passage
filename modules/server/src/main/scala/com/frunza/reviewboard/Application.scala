package com.frunza.reviewboard

import com.frunza.reviewboard.http.HttpApi
import com.frunza.reviewboard.repositories.{CompanyRepositoryLive, Repository}
import com.frunza.reviewboard.services.CompanyServiceLive
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
      CompanyRepositoryLive.layer,
      CompanyServiceLive.layer,
      Repository.dataLayer
    )

}
