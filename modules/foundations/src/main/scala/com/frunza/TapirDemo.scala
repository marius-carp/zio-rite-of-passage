package com.frunza

import zio.*
import sttp.tapir.*
import sttp.tapir.json.zio.jsonBody
import sttp.tapir.server.ziohttp.{ZioHttpInterpreter, ZioHttpServerOptions}
import zio.http.Server
import zio.json.{DeriveJsonCodec, JsonCodec}
import sttp.tapir.generic.auto.*
import sttp.tapir.server.ServerEndpoint

import scala.collection.mutable


object TapirDemo extends ZIOAppDefault {

  val simplestEndpoint = endpoint
    .tag("simple")
    .name("simple")
    .description("simplest endpoint possible")
    .get
    .in("simple") //path
    .out(plainBody[String]) //output
    .serverLogicSuccess[Task](_ => ZIO.succeed("All good!"))

  val simpleServerProgram = Server.serve(
    ZioHttpInterpreter(
      ZioHttpServerOptions.default // can add configs e.g. CORS
    ).toHttp(simplestEndpoint)
  )


  val db: mutable.Map[Long, Job] = mutable.Map(
    1L -> Job(1L, "Instructor", "frunza.com", "Frunza is the company")
  )


  val createEndpoint: ServerEndpoint[Any, Task] = endpoint
    .tag("jobs")
    .name("getAll")
    .description("get all jobs")
    .in("jobs")
    .post
    .in(jsonBody[CreateJobRequest])
    .out(jsonBody[Job])
    .serverLogicSuccess(req => ZIO.succeed {
      val newId = db.keys.max + 1
      val newJob = Job(newId, req.title, req.url, req.company)
      db += (newId -> newJob)

      newJob
    })

  val getByIdEndpoint: ServerEndpoint[Any, Task] = endpoint
    .tag("jobs")
    .name("getById")
    .description("Get job by id")
    .in("jobs" / path[Long]("id"))
    .get
    .out(jsonBody[Option[Job]])
    .serverLogicSuccess(id => ZIO.succeed(db.get(id)))

  val getAllEndpoint: ServerEndpoint[Any, Task] = endpoint
    .tag("jobs")
    .name("getAll")
    .description("get all jobs")
    .get
    .out(jsonBody[List[Job]])
    .serverLogicSuccess(_ => ZIO.succeed(db.values.toList))


  val serverProgram = Server.serve(
    ZioHttpInterpreter(
      ZioHttpServerOptions.default
    ).toHttp(List(createEndpoint, getByIdEndpoint, getAllEndpoint))
  )

  override def run = serverProgram.provide(
    Server.default
  )
}

case class Job(id: Long, title: String, url: String, company: String)

object Job {
  given codec: JsonCodec[Job] = DeriveJsonCodec.gen[Job] // macro based JSON codec
}

case class CreateJobRequest(title: String, url: String, company: String)

object CreateJobRequest {
  given codec: JsonCodec[CreateJobRequest] = DeriveJsonCodec.gen[CreateJobRequest]
}
