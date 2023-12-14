package com.frunza.reviewboard.services

import zio.*
import com.frunza.reviewboard.domain.data.*
import com.frunza.reviewboard.http.requests.CreateCompanyRequest

import scala.collection.mutable

trait CompanyService {
  def create(req: CreateCompanyRequest): Task[Company]
  def getAll: Task[List[Company]]
  def getById(id: Long): Task[Option[Company]]
  def getBySlug(slug: String): Task[Option[Company]]
  
}

object CompanyService {
  val dummyLayer = ZLayer.succeed(new CompanyServiceDummy)
}

class CompanyServiceDummy extends CompanyService {
  private val db = mutable.Map[Long, Company]()

  override def create(req: CreateCompanyRequest): Task[Company] = ZIO.succeed {
    val newId = db.keys.maxOption.getOrElse(0L) + 1L
    val newCompany = req.toCompany(newId)
    db += newId -> newCompany

    newCompany
  }

  override def getAll: Task[List[Company]] = 
    ZIO.succeed(db.values.toList)

  override def getById(id: Long): Task[Option[Company]] =
    ZIO.succeed(db.get(id))

  override def getBySlug(slug: String): Task[Option[Company]] =
    ZIO.succeed(db.values.find(_.slug == slug))
}


