package com.frunza.reviewboard

import com.frunza.reviewboard.http.HttpApi
import com.frunza.reviewboard.repositories.{CompanyRepositoryLive, Repository, ReviewRepositoryLive}
import com.frunza.reviewboard.services.{CompanyServiceLive, ReviewServiceLive}
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
      //services
      CompanyServiceLive.layer,
      ReviewServiceLive.layer,
      // other
      Repository.dataLayer
    )

}
