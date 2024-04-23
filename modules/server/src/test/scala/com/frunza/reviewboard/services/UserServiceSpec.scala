package com.frunza.reviewboard.services

import com.frunza.reviewboard.domain.data.{User, UserID, UserToken}
import com.frunza.reviewboard.repositories.UserRepository
import zio.*
import zio.test.*

object UserServiceSpec extends ZIOSpecDefault {

  val user = User(1L, "marius@frunza.com", "1000:AF726B865B287ABC404987C369118D28DEA120B0D82AF679:AD584110E60D5665E05AD801F9E7015ABEB1F5396CEE7981")

  val stubRepoLayer = ZLayer.succeed(
    new UserRepository {
      val db = collection.mutable.Map[Long, User](1L -> user)

      override def create(user: User): Task[User] = ZIO.succeed {
        db += (user.id -> user)
        user
      }

      override def update(id: Long, op: User => User): Task[User] = ZIO.attempt {
        val newUser = op(db(id))
        db += (newUser.id -> newUser)
        newUser
      }

      override def getById(id: Long): Task[Option[User]] = ZIO.succeed(db.get(id))

      override def getByEmail(email: String): Task[Option[User]] = ZIO.succeed(db.values.find(_.email == email))

      override def delete(id: Long): Task[User] = ZIO.attempt {
        val user = db(id)
        db -= id
        user
      }
    }
  )

  val stubJwtLayer = ZLayer.succeed {
    new JWTService:
      override def createToken(user: User): Task[UserToken] =
        ZIO.succeed(UserToken(user.email, "ACCESS", Long.MaxValue))

      override def verifyToken(token: String): Task[UserID] =
        ZIO.succeed(UserID(user.id, user.email))
  }


  override def spec: Spec[TestEnvironment & Scope, Any] =
    suite("UserServiceSpec")(
      test("create and validate a user") {
        for {
          service <- ZIO.service[UserService]
          registeredUser <- service.registerUser(user.email, "password")
          valid <- service.verifyPassword(registeredUser.email, "password")
        } yield assertTrue(valid && user.email == registeredUser.email)
      },
      test("validate correct credentials") {
        for {
          service <- ZIO.service[UserService]
          valid <- service.verifyPassword(user.email, "password")
        } yield assertTrue(valid)
      },
      test("validate incorrect credentials") {
        for {
          service <- ZIO.service[UserService]
          valid <- service.verifyPassword(user.email, "wrong-password")
        } yield assertTrue(!valid)
      },
      test("invalidate non-existent user") {
        for {
          service <- ZIO.service[UserService]
          valid <- service.verifyPassword("someemail@email.com", "some-password")
        } yield assertTrue(!valid)
      },
      test("update password") {
        for {
          service <- ZIO.service[UserService]
          newUser <- service.updatePassword(user.email, "password", "scalarulez")
          oldValid <- service.verifyPassword(user.email, "password")
          newValid <- service.verifyPassword(user.email, "scalarulez")
        } yield assertTrue(newValid, !oldValid)
      },
      test("delete with non-existent user should fail") {
        for {
          service <- ZIO.service[UserService]
          err <- service.deleteUser("someemail@email.com", "some-password").flip
        } yield assertTrue(err.isInstanceOf[RuntimeException])
      },
      test("delete user with incorrect password should fail") {
        for {
          service <- ZIO.service[UserService]
          err <- service.deleteUser(user.email, "some-password").flip
        } yield assertTrue(err.isInstanceOf[RuntimeException])
      },
      test("delete user") {
        for {
          service <- ZIO.service[UserService]
          deletedUser <- service.deleteUser(user.email, "password")
        } yield assertTrue(user.email == deletedUser.email)
      }
    ).provide(
      UserServiceLive.layer,
      stubJwtLayer,
      stubRepoLayer
    )

}
