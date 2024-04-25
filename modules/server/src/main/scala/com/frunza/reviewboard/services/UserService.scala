package com.frunza.reviewboard.services

import com.frunza.reviewboard.domain.data.{User, UserToken}
import com.frunza.reviewboard.repositories.{RecoveryTokensRepository, UserRepository}
import com.frunza.reviewboard.services.UserServiceLive.Hasher
import zio.*

import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

trait UserService {

  def registerUser(email: String, password: String): Task[User]
  def verifyPassword(email: String, password: String): Task[Boolean]
  def updatePassword(email: String, oldPassword: String, newPassword: String): Task[User]
  def deleteUser(email: String, password: String): Task[User]

  def generateToken(email: String, password: String): Task[Option[UserToken]]

  def sendPasswordRecoveryToken(email: String): Task[Unit]
  def recoverPasswordFromToken(email: String, token: String, newPassword: String): Task[Boolean]

}

class UserServiceLive private(jwtService: JWTService, emailService: EmailService, userRepo: UserRepository, tokensRepo: RecoveryTokensRepository) extends UserService {

  override def registerUser(email: String, password: String): Task[User] =
    userRepo.create(
      User (
        id = -1L,
        email = email,
        hashedPassword = Hasher.generateHash(password)
      )
    )
  override def verifyPassword(email: String, password: String): Task[Boolean] =
    for {
      existingUser <- userRepo
        .getByEmail(email)
      result <- existingUser match {
        case Some(user) =>
          ZIO
          .attempt(Hasher.validateHash(password, user.hashedPassword))
          .orElseSucceed(false)
        case None =>
          ZIO.succeed(false)
      }
    } yield result

  def generateToken(email: String, password: String): Task[Option[UserToken]] = {
    for {
      existingUser <- userRepo
        .getByEmail(email)
        .someOrFail(new RuntimeException(s"cannot verify user $email: nonexistant"))
      verified <- ZIO.attempt(
        Hasher.validateHash(password, existingUser.hashedPassword)
      )
      maybeToken <- jwtService.createToken(existingUser).when(verified)
    } yield maybeToken
  }

  override def updatePassword(email: String, oldPassword: String, newPassword: String): Task[User] = {
    for {
      user <- userRepo.
        getByEmail(email)
        .someOrFail(new RuntimeException(s"cannot verify user $email: nonexistant"))
      verify <- ZIO.attempt(
        UserServiceLive.Hasher.validateHash(oldPassword, user.hashedPassword)
      )
      updatedUser <- userRepo.update(
        user.id,
        user => user.copy(hashedPassword = UserServiceLive.Hasher.generateHash(newPassword))
      ).when(verify)
        .someOrFail(new RuntimeException(s"could not update password for $email"))
    } yield updatedUser
  }

  override def deleteUser(email: String, password: String): Task[User] = for {
    user <- userRepo.
      getByEmail(email)
      .someOrFail(new RuntimeException(s"cannot verify user $email: nonexistant"))
    verify <- ZIO.attempt(
      UserServiceLive.Hasher.validateHash(password, user.hashedPassword)
    )
    updatedUser <- userRepo
      .delete(user.id)
      .when(verify)
      .someOrFail(new RuntimeException(s"could not update password for $email"))
  } yield updatedUser

  override def sendPasswordRecoveryToken(email: String): Task[Unit] = {
    tokensRepo.getToken(email).flatMap {
      case Some(token) => emailService.sendPasswordRecoveryEmail(email, token)
      case None  => ZIO.unit
    }
  }

  override def recoverPasswordFromToken(email: String, token: String, newPassword: String): Task[Boolean] =
    for {
      existingUser <- userRepo.getByEmail(email).someOrFail(new RuntimeException("Non existent user"))
      tokenIsValid <- tokensRepo.checkToken(email, token)
      result <- userRepo
        .update(existingUser.id, user => user.copy(hashedPassword = UserServiceLive.Hasher.generateHash(newPassword)))
        .when(tokenIsValid)
        .map(_.nonEmpty)
    } yield result
}

object UserServiceLive {
  val layer = ZLayer {
    for {
      jwtService <- ZIO.service[JWTService]
      repo <- ZIO.service[UserRepository]
      emailService <- ZIO.service[EmailService]
      recoveryTokenRepo <- ZIO.service[RecoveryTokensRepository]
    } yield new UserServiceLive(jwtService, emailService, repo, recoveryTokenRepo)
  }

  object Hasher {
    private val PBKDF2_ALGO: String = "PBKDF2WithHmacSHA512"
    private val PBKDF2_ITERATIONS: Int = 1000
    private val SALT_BYTE_SIZE: Int = 24
    private val HASH_BYTE_SIZE: Int = 24
    private val skf: SecretKeyFactory = SecretKeyFactory.getInstance(PBKDF2_ALGO)

    def generateHash(string: String): String = {
      val rng: SecureRandom = new SecureRandom()
      val salt: Array[Byte] = Array.ofDim[Byte](SALT_BYTE_SIZE)
      rng.nextBytes(salt) // create 24 ransom bytes
      val hashBytes = pbkdf2(string.toCharArray, salt, PBKDF2_ITERATIONS, HASH_BYTE_SIZE)

      s"$PBKDF2_ITERATIONS:${toHex(salt)}:${toHex(hashBytes)}"
    }

    def validateHash(string: String, hash: String): Boolean = {
      val hashSegments = hash.split(":")
      val nIterations = hashSegments(0).toInt
      val salt = fromHex(hashSegments(1))
      val validHash = fromHex(hashSegments(2))

      val testHash = pbkdf2(string.toCharArray, salt, nIterations, HASH_BYTE_SIZE)

      compareBytes(testHash, validHash)
    }

    private def compareBytes(a: Array[Byte], b: Array[Byte]): Boolean = {
      val range = 0 until math.min(a.length, b.length)
      val diff = range.foldLeft(a.length ^ b.length) {
        case (acc, i) => acc | (a(i) ^ b(i))
      }

      diff == 0
    }

    private def pbkdf2(message: Array[Char], salt: Array[Byte], iterations: Int, nBytes: Int): Array[Byte] = {
      val keySpec: PBEKeySpec = new PBEKeySpec(message, salt, iterations, nBytes * 8)
      skf.generateSecret(keySpec).getEncoded
    }

    private def fromHex(string: String): Array[Byte] = {
      string.sliding(2, 2).toArray.map { hexValue =>
        Integer.parseInt(hexValue, 16).toByte
      }
    }

    private def toHex(array: Array[Byte]): String =
      array.map(b => "%02X".format(b)).mkString
  }
}
