package com.frunza.reviewboard.config

import com.typesafe.config.ConfigFactory
import zio.{Tag, ZIO, ZLayer}
import zio.config.magnolia.*
import zio.config.*
import zio.config.typesafe.TypesafeConfig

object Configs {
  def makeLayer[C](path: String)(using desc: Descriptor[C], tag: Tag[C]): ZLayer[Any, Throwable, C] = {
    TypesafeConfig.fromTypesafeConfig(
      ZIO.attempt(ConfigFactory.load().getConfig(path)),
      descriptor[C]
    )
  }
}
