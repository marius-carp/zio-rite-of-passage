package com.frunza

import zio.*

import scala.io.StdIn

object ZIORecap extends ZIOAppDefault {

  // "effects"
  val meaningOfLife: ZIO[Any, Nothing, Int] = ZIO.succeed(42)
  // fail
  val aFailure: ZIO[Any, String, Nothing] = ZIO.fail("Smth went wrong")
  // suspension
  val aSuspension: ZIO[Any, Throwable, Int] = ZIO.suspend(meaningOfLife)

  val improvedMOL = meaningOfLife.map(_ * 2)
  val printinMOL = meaningOfLife.flatMap(mol => ZIO.succeed(println(mol)))
  val smallProgram = for {
    _ <- Console.printLine("what's  your name")
    name <- ZIO.succeed(StdIn.readLine())
    _ <- Console.printLine(s"Welcome to ZIO, $name")
  } yield ()

  // error handling
  val anAttempt: ZIO[Any, Throwable, Int] = ZIO.attempt {
    println("tryint smething")
    val string: String = null
    string.length
  }

  // cath errors effectfully
  val catchError = anAttempt.catchAll(e => ZIO.succeed("Returing some different value"))
  val catchSelective = anAttempt.catchSome {
    case e: RuntimeException => ZIO.succeed(s"Ignoring runtime excepton ${e}")
    case _ => ZIO.succeed("Ignoring everything else")
  }

  // fibers
  val delayedValue = ZIO.sleep(1.second) *> Random.nextIntBetween(0, 100)
  val aPair = for {
    a <- delayedValue
    b <- delayedValue
  } yield (a, b) //this takes 2 sec

  val aPairPar = for {
    fibA <- delayedValue.fork
    fibB <- delayedValue.fork
    a <- fibA.join
    b <- fibB.join
  } yield(a, b) //this takes 1 sec

  val interruptedFiber = for {
    fib <- delayedValue.onInterrupt(ZIO.succeed(println("I'm interrupted"))).fork
    _ <- ZIO.sleep(500.millis) *> ZIO.succeed(println("canceling fiber")) *> fib.interrupt
    _ <- fib.join
  } yield ()

  val ignoredInterruption = for {
    fib <- ZIO.uninterruptible(delayedValue.onInterrupt(ZIO.succeed(println("I'm interrupted")))).fork
    _ <- ZIO.sleep(500.millis) *> ZIO.succeed(println("canceling fiber")) *> fib.interrupt
    _ <- fib.join
  } yield ()

  // many APIs
  val aPairPar_v2 = delayedValue.zipPar(delayedValue)
  val randomx10 = ZIO.collectAllPar((1 to 10).map(_ => delayedValue)) //traverse

  // dependencies

  case class User(name: String, email: String)
  class UserSubscription(emailService: EmailService, userDatabase: UserDatabase) {
    def subscribeUser(user: User): Task[Unit] = for {
      _ <- emailService.email(user)
      _ <- userDatabase.insert(user)
      _ <- ZIO.succeed(s"subscribed $user")
    } yield ()
  }
  object UserSubscription {
    val live: ZLayer[EmailService with UserDatabase, Nothing, UserSubscription] =
      ZLayer.fromFunction(new UserSubscription(_, _))
  }

  class EmailService {
    def email(user: User): Task[Unit] = ZIO.succeed(s"Emaild $user")
  }
  object EmailService {
    val live: ZLayer[Any, Nothing, EmailService] = ZLayer.succeed(new EmailService)
  }

  class UserDatabase(connectionPool: ConnectionPool) {
    def insert(user: User): Task[Unit] = ZIO.succeed(s"inserted $user")
  }
  object UserDatabase {
    val live: ZLayer[ConnectionPool, Nothing, UserDatabase] =
      ZLayer.fromFunction(new UserDatabase(_))
  }

  class ConnectionPool(nConnection: Int) {
    def get: Task[Connection] = ZIO.succeed(Connection())
  }
  object ConnectionPool {
    def live(nConnection: Int): ZLayer[Any, Nothing, ConnectionPool] =
      ZLayer.succeed(ConnectionPool(nConnection))
  }

  case class Connection()

  def subscribe(user: User): ZIO[UserSubscription, Throwable, Unit] = for {
    sub <- ZIO.service[UserSubscription]
    _ <- sub.subscribeUser(user)
  } yield()

  val program = for {
    _ <- subscribe(User("Marius", "marius@frunza.com"))
    _ <- subscribe(User("Nimeni Altu'", "nimeni@frunza.com"))
  } yield()

  override def run = program.provide(
    ConnectionPool.live(10),
    UserDatabase.live,
    EmailService.live,
    UserSubscription.live
  )
}
