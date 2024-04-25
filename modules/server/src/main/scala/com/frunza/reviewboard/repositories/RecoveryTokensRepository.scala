package com.frunza.reviewboard.repositories


import com.frunza.reviewboard.config.{Configs, RecoveryTokensConfig}
import com.frunza.reviewboard.domain.data.PasswordRecoveryToken
import io.getquill.*
import io.getquill.jdbczio.Quill
import zio.*

trait RecoveryTokensRepository {

  def getToken(email: String): Task[Option[String]]
  def checkToken(email: String, token: String): Task[Boolean]

}

class RecoveryTokenRepositoryLive private(tokenConfig: RecoveryTokensConfig, quill: Quill.Postgres[SnakeCase], userRepo: UserRepository) extends RecoveryTokensRepository {
  import quill.*

  inline given schema: SchemaMeta[PasswordRecoveryToken] = schemaMeta[PasswordRecoveryToken]("recovery_tokens")
  inline given insMeta: InsertMeta[PasswordRecoveryToken] = insertMeta[PasswordRecoveryToken]()
  inline given upMeta: UpdateMeta[PasswordRecoveryToken] = updateMeta[PasswordRecoveryToken]()

  private val tokenDuration = 600000 // TODO pass from config

  override def getToken(email: String): Task[Option[String]] =
    userRepo.getByEmail(email)
      .flatMap {
        case Some(_) => makeFreshToken(email).map(Some(_))
        case None => ZIO.none
      }

  override def checkToken(email: String, token: String): Task[Boolean] =
    run(query[PasswordRecoveryToken].filter(r => r.email == lift(email) && r.token == lift(token)))
      .map(_.nonEmpty)

  private def makeFreshToken(email: String): Task[String] =
    findToken(email).flatMap {
      case Some(_) => replaceToken(email)
      case None => generateToken(email)
    }

  private def findToken(email: String): Task[Option[String]] =
    run(query[PasswordRecoveryToken]
      .filter(_.email == lift(email)))
      .map(_.headOption.map(_.token))

  private def replaceToken(email: String): Task[String] =
    for {
      token <- randomUpperCaseString(8)
      _ <- run(query[PasswordRecoveryToken]
        .updateValue(lift(PasswordRecoveryToken(email, token, java.lang.System.currentTimeMillis() + tokenDuration)))
        .returning(r => r))
    } yield token

  private def generateToken(email: String): Task[String] =
    for {
      token <- randomUpperCaseString(8)
      _ <- run(query[PasswordRecoveryToken]
        .insertValue(lift(PasswordRecoveryToken(email, token, java.lang.System.currentTimeMillis() + tokenDuration)))
        .returning(r => r))
    } yield token

  private def randomUpperCaseString(len: Int): Task[String] =
    ZIO.succeed(scala.util.Random.alphanumeric.take(len).mkString.toUpperCase)
}

object RecoveryTokenRepositoryLive {
  val layer = ZLayer {
    for {
      config <- ZIO.service[RecoveryTokensConfig]
      quill <- ZIO.service[Quill.Postgres[SnakeCase]]
      userRepo <- ZIO.service[UserRepository]
    } yield new RecoveryTokenRepositoryLive(config, quill, userRepo)
  }

  val configuredLayer = Configs.makeLayer[RecoveryTokensConfig]("frunza.recoveryTokens") >>> layer
}
