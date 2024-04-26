package com.frunza.reviewboard.services

import com.frunza.reviewboard.config.{Configs, EmailServiceConfig}
import zio.*

import java.util.Properties
import javax.mail.internet.MimeMessage
import javax.mail.{Authenticator, Message, PasswordAuthentication, Session, Transport}

trait EmailService {

  def sendEmail(to: String, subject: String, content: String): Task[Unit]
  def sendPasswordRecoveryEmail(to: String, token: String): Task[Unit]

}

class EmailServiceLive private (config: EmailServiceConfig) extends EmailService {

  override def sendEmail(to: String, subject: String, content: String): Task[Unit] = {
    val message = for {
      prop <- propsResources
      session <- createSession(prop)
      message <- createMessage(session)("marius@frunza.com", to, subject, content)
    } yield message

    message.map(msg => Transport.send(msg))
  }

  override def sendPasswordRecoveryEmail(to: String, token: String): Task[Unit] = {
    val subject = "Frunza: Password Recovery"
    val content = s"""
         <div style= "
          border 1px solid black;
          padding: 20px;
          font-family: sans-serif;
          lin-height: 2;
          font-size: 20px;
         ">
          <h1>Frunza: Password Recovery</h1>
          <p>Your password recovery token is <strong>$token</strong></p>
          <p>from Frunza</p>
         </div>
       """

    sendEmail(to, subject, content)
  }

  private val propsResources: Task[Properties] = {
    val prop = new Properties
    prop.put("mail.smtp.auth", true)
    prop.put("mail.smtp.starttls.enable", "true")
    prop.put("mail.smtp.host", config.host)
    prop.put("mail.smtp.port", config.port)
    prop.put("mail.smtp.ssl.trust", config.host)

    ZIO.succeed(prop)
  }

  private def createSession(prop: Properties): Task[Session] = ZIO.attempt {
    Session.getInstance(prop, new Authenticator {
      override def getPasswordAuthentication: PasswordAuthentication = {
        new PasswordAuthentication(config.user, config.pass)
      }
    })
  }

  private def createMessage(session: Session)(from: String, to: String, subject: String, content: String): Task[MimeMessage] = {
    val message = new MimeMessage(session)
    message.setFrom(from)
    message.setRecipients(Message.RecipientType.TO, to)
    message.setSubject(subject)
    message.setContent(content, "text/html; charset=utf-8")

    ZIO.succeed(message)
  }

}

object EmailServiceLive {
  val layer = ZLayer {
    ZIO.service[EmailServiceConfig].map(new EmailServiceLive(_))
  }

  val configuredLayer = Configs.makeLayer[EmailServiceConfig]("frunza.email") >>> layer
}

object EmailServiceDemo extends ZIOAppDefault {
  val program = for {
    emailService <- ZIO.service[EmailService]
    _ <- emailService.sendPasswordRecoveryEmail("je@frunza.com", "ABCD1234")
    _ <- Console.printLine("Email done")
  } yield ()

  override def run: ZIO[Any with ZIOAppArgs with Scope, Any, Any] =
    program.provide(EmailServiceLive.configuredLayer)
}
