package com.frunza.reviewboard.services

import zio.{Task, ZLayer}

trait EmailService {

  def sendEmail(to: String, subject: String, content: String): Task[Unit]
  def sendPasswordRecoveryEmail(to: String, token: String): Task[Unit]

}

class EmailServiceLive private extends EmailService {


  override def sendEmail(to: String, subject: String, content: String): Task[Unit] = ???

  override def sendPasswordRecoveryEmail(to: String, token: String): Task[Unit] = ???
}

object EmailServiceLive {
  val layer = ZLayer.succeed(new EmailServiceLive)
}
