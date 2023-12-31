package com.frunza.reviewboard.repositories

import io.getquill.SnakeCase
import io.getquill.jdbczio.Quill
import zio.ZLayer

object Repository {
  def quillLayer = Quill.Postgres.fromNamingStrategy(SnakeCase)
  def dataSourceLayer = Quill.DataSource.fromPrefix("frunza.db")

  val dataLayer: ZLayer[Any, Throwable, Quill.Postgres[SnakeCase.type]] = dataSourceLayer >>> quillLayer
}
