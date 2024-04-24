package com.frunza.reviewboard.http.controllers

import com.frunza.reviewboard.domain.data.UserID
import com.frunza.reviewboard.domain.errors.UnauthorizedException
import com.frunza.reviewboard.http.endpoints.UserEndpoints
import com.frunza.reviewboard.http.responses.UserResponse
import com.frunza.reviewboard.services.{JWTService, UserService}
import sttp.tapir.server.ServerEndpoint
import zio.*
import sttp.tapir.*

class UserController private (userService: UserService, jwtService: JWTService) extends BaseController with UserEndpoints {

  val create: ServerEndpoint[Any, Task] = createUserEndpoint
    .serverLogic { req =>
      userService.registerUser(req.email, req.password)
        .map(user => UserResponse(user.email))
        .either
    }

  val login: ServerEndpoint[Any, Task] = loginEndpoint
    .serverLogic { req =>
      userService
        .generateToken(req.email, req.password)
        .someOrFail(UnauthorizedException)
        .either
    }

  val changePassword: ServerEndpoint[Any, Task] = updatePasswordEndpoint
    .serverSecurityLogic[UserID, Task](token => jwtService.verifyToken(token).either)
    .serverLogic { userId => req =>
      userService
        .updatePassword(req.email, req.oldPassword, req.newPassword)
        .map(user => UserResponse(user.email))
        .either
    }

  val delete: ServerEndpoint[Any, Task] = deleteEndpoint
    .serverSecurityLogic[UserID, Task](token => jwtService.verifyToken(token).either)
    .serverLogic { userId => req =>
      userService
        .deleteUser(req.email, req.password)
        .map(user => UserResponse(user.email))
        .either
    }

  override val routes: List[ServerEndpoint[Any, Task]] = List(
    create,
    changePassword,
    delete,
    login
  )
}

object UserController {
  val makeZIO = for {
    userService <- ZIO.service[UserService]
    jwtService <- ZIO.service[JWTService]
  } yield UserController(userService, jwtService)
}
