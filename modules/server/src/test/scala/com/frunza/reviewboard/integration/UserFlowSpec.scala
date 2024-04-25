package com.frunza.reviewboard.integration


import com.frunza.reviewboard.config.JWTConfig
import com.frunza.reviewboard.domain.data.UserToken
import com.frunza.reviewboard.http.controllers.UserController
import com.frunza.reviewboard.http.requests.{CreateReviewRequest, DeleteAccountRequest, LoginRequest, RegisterUserAccount, UpdatePasswordRequest}
import com.frunza.reviewboard.http.responses.UserResponse
import com.frunza.reviewboard.repositories.{Repository, UserRepository, UserRepositoryLive}
import com.frunza.reviewboard.services.{JWTServiceLive, UserServiceLive}
import sttp.client3.*
import sttp.client3.testing.SttpBackendStub
import sttp.model.Method
import sttp.monad.MonadError
import sttp.tapir.server.stub.TapirStubInterpreter
import sttp.tapir.ztapir.RIOMonadError
import zio.*
import zio.json.*
import zio.test.*

import scala.com.frunza.reviewboard.repositories.RepositorySpec


object UserFlowSpec extends ZIOSpecDefault with RepositorySpec {

  override val initScript: String = "sql/integration.sql"
  private given zioMonadError: MonadError[Task] = new RIOMonadError[Any]

  private def backendStubZIO =
    for {
      controller <- UserController.makeZIO
      backendStub <- ZIO.succeed(
        TapirStubInterpreter(SttpBackendStub(MonadError[Task]))
        .whenServerEndpointsRunLogic(controller.routes)
        .backend()
      )
    } yield backendStub

  extension [A: JsonCodec] (backend: SttpBackend[Task, Nothing]) {
    def sendRequest[B: JsonCodec](
      method: Method,
      path: String,
      payload: A,
      maybeToken: Option[String] = None,
    ): Task[Option[B]] = {
      basicRequest
        .method(method, uri"$path")
        .body(payload.toJson)
        .auth.bearer(maybeToken.getOrElse(""))
        .send(backend)
        .map(_.body)
        .map(_.toOption.flatMap(payload => payload.fromJson[B].toOption))
    }

    def post[B: JsonCodec](path: String, payload: A): Task[Option[B]] =
      sendRequest(Method.POST, path, payload)

    def postAuth[B: JsonCodec](path: String, payload: A, token: String): Task[Option[B]] =
      sendRequest(Method.POST, path, payload, Some(token))

    def put[B: JsonCodec](path: String, payload: A): Task[Option[B]] =
      sendRequest(Method.PUT, path, payload)

    def putAuth[B: JsonCodec](path: String, payload: A, token: String): Task[Option[B]] =
      sendRequest(Method.PUT, path, payload, Some(token))

    def delete[B: JsonCodec](path: String, payload: A): Task[Option[B]] =
      sendRequest(Method.DELETE, path, payload)

    def deleteAuth[B: JsonCodec](path: String, payload: A, token: String): Task[Option[B]] =
      sendRequest(Method.DELETE, path, payload, Some(token))
  }

  override def spec: Spec[TestEnvironment with Scope, Any] =
    suite("UserFlowSpec")(
      test("create user") {
        for {
          backendStub <- backendStubZIO
          maybeResponse <- backendStub.post[UserResponse](
            "/users",
            RegisterUserAccount("marius@frunza.com", "password"),
          )
        } yield assertTrue(maybeResponse.contains(UserResponse("marius@frunza.com")))
      },
      test("create and login") {
        for {
          backendStub <- backendStubZIO
          maybeResponse <- backendStub.post[UserResponse](
            "/users",
             RegisterUserAccount("marius@frunza.com", "password")
          )
          maybeToken <- backendStub.post[UserToken](
              "/users/login",
              LoginRequest("marius@frunza.com", "password")
          )
        } yield assertTrue(maybeToken.exists(_.email == "marius@frunza.com"))
      },
      test("change password") {
        for {
          backendStub <- backendStubZIO
          maybeResponse <- backendStub.post[UserResponse](
            "/users",
            RegisterUserAccount("marius@frunza.com", "password")
          )
          userToken <- backendStub.post[UserToken](
            "/users/login",
            LoginRequest("marius@frunza.com", "password")
          ).someOrFail(new RuntimeException("Authentication failed"))
          resp <- backendStub.putAuth[UserResponse](
            "/users/password",
            UpdatePasswordRequest("marius@frunza.com", "password", "newPassword"),
            userToken.token)
          maybeOldToken <- backendStub.post[UserToken](
            "/users/login",
            LoginRequest("marius@frunza.com", "password"))
          maybeNewToken <- backendStub.post[UserToken](
            "/users/login",
            LoginRequest("marius@frunza.com", "newPassword"))

        } yield assertTrue(maybeOldToken.isEmpty && maybeNewToken.nonEmpty)
      },
      test("delete user") {
        for {
          backendStub <- backendStubZIO
          maybeResponse <- backendStub.post[UserResponse](
            "/users",
            RegisterUserAccount("marius@frunza.com", "password")
          )
          userRepo <- ZIO.service[UserRepository]
          maybeOldUser <- userRepo.getByEmail("marius@frunza.com")
          userToken <- backendStub.post[UserToken](
            "/users/login",
            LoginRequest("marius@frunza.com", "password")
          ).someOrFail(new RuntimeException("Authentication failed"))
          resp <- backendStub.deleteAuth[UserResponse](
            "/users",
            DeleteAccountRequest("marius@frunza.com", "password"),
            userToken.token)
          maybeUser <- userRepo.getByEmail("marius@frunza.com")
        } yield assertTrue(maybeOldUser.exists(_.email == "marius@frunza.com") && maybeUser.isEmpty)
      }
    ).provide(
      UserServiceLive.layer,
      JWTServiceLive.layer,
      UserRepositoryLive.layer,
      Repository.quillLayer,
      dataSourceLayer,
      ZLayer.succeed(JWTConfig("secret", 3600)),
      Scope.default
    )

}
