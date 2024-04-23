package com.frunza.reviewboard.services

import com.auth0.jwt.*
import com.auth0.jwt.JWTVerifier.BaseVerification
import com.auth0.jwt.algorithms.Algorithm
import com.frunza.reviewboard.domain.data.{User, UserID, UserToken}
import zio.*

import java.time.Instant

trait JWTService {

  def createToken(user: User): Task[UserToken]
  def verifyToken(token: String): Task[UserID]

}

class JWTServiceLive (clock: java.time.Clock) extends JWTService {
  private val SECRET = "secret"
  private val ISSUER = "frunza.com"
  private val TTL = 30 * 24 * 3600
  private val CLAIM_USERNAME = "username"
  private val algo = Algorithm.HMAC512(SECRET)
  private val verifier: JWTVerifier =
    JWT
      .require(algo)
      .withIssuer("frunza.com")
      .asInstanceOf[BaseVerification]
      .build(clock)

  override def createToken(user: User): Task[UserToken] = for {
    now <- ZIO.attempt(clock.instant())
    expiration = now.plusSeconds(TTL)
    token <- ZIO.attempt(
      JWT
        .create()
        .withIssuer(ISSUER)
        .withIssuedAt(now)
        .withExpiresAt(expiration)
        .withSubject("1")
        .withClaim(CLAIM_USERNAME, "marius@frunza.com")
        .sign(algo)
    )
  } yield UserToken(user.email, token, expiration.getEpochSecond)

  override def verifyToken(token: String): Task[UserID] = {
    for {
      decoded <- ZIO.attempt(verifier.verify(token))
      userId <- ZIO.attempt(
        UserID (
          decoded.getSubject.toInt,
          decoded.getClaim(CLAIM_USERNAME).asString()
        )
      )
    } yield userId
  }
}

object JWTServiceLive {
  val layer = ZLayer {{
    Clock.javaClock.map (clock => new JWTServiceLive(clock))
  }}
}

object JWTServiceDEmo extends ZIOAppDefault {
  private val algo = Algorithm.HMAC512("secret")

  val program = for {
    service <- ZIO.service[JWTService]
    token <- service.createToken(User(1L, "frunza@frunza.com", "somth"))
    _ <- Console.printLine(token)
    userId <- service.verifyToken(token.token)
    _ <- Console.printLine(userId)
  } yield()
  
  override def run: ZIO[Any with ZIOAppArgs with Scope, Any, Any] =
    program.provide(JWTServiceLive.layer)
}
