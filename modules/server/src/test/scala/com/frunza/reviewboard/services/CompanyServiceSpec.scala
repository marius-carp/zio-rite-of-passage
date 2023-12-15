package scala.com.frunza.reviewboard.services

import com.frunza.reviewboard.domain.data.Company
import com.frunza.reviewboard.http.requests.CreateCompanyRequest
import com.frunza.reviewboard.repositories.CompanyRepository
import com.frunza.reviewboard.services.{CompanyService, CompanyServiceLive}
import zio.*
import zio.test.*

import scala.collection.immutable.List
import scala.collection.mutable
import scala.com.frunza.reviewboard.syntax.*

object CompanyServiceSpec extends ZIOSpecDefault {

  val service = ZIO.serviceWithZIO[CompanyService]
  val stubRepoLayer = ZLayer.succeed (
    new CompanyRepository {
      private val db = mutable.Map[Long, Company]()
      override def create(company: Company): Task[Company] = ZIO.succeed {
        val newId = db.keys.maxOption.getOrElse(0L) + 1L
        val newCompany = company.copy(id = newId)
        db += newId -> newCompany

        newCompany
      }

      override def delete(id: Long): Task[Company] =
        ZIO.attempt( {
          val company = db(id)
          db -= id

          company
        })

      override def get: Task[List[Company]] =
        ZIO.succeed(db.values.toList)

      override def getById(id: Long): Task[Option[Company]] =
        ZIO.succeed(db.get(id))

      override def getBySlug(slug: String): Task[Option[Company]] =
        ZIO.succeed(db.values.find(_.slug == slug))

      override def update(id: Long, op: Company => Company): Task[Company] =
        ZIO.attempt {
          val company = db(id)
          val newCompany = op(company)
          db += (id -> newCompany)

          newCompany
        }
    }
  )

  override def spec: Spec[TestEnvironment with Scope, Any] =
    suite("CompanyServiceTest")(
      test("create") {
        val companyZIO = service(_.create(CreateCompanyRequest("Frunza Company", "frunza.com")))

        companyZIO.assert { company =>
          company.name == "Frunza Company" &&
          company.url == "frunza.com" &&
          company.slug == "frunza-company"
        }
      },

      test("get by id") {
        val program = for {
          company <- service(_.create(CreateCompanyRequest("Frunza Company", "frunza.com")))
          companyOpt <- service(_.getById(company.id))
        } yield (company, companyOpt)

        program.assert {
          case (company, Some(companyRes)) =>
            company.name == "Frunza Company" &&
            company.url == "frunza.com" &&
            company.slug == "frunza-company" &&
            company == companyRes
          case _ => false
        }
      },

      test("get by slug") {
        val program = for {
          company <- service(_.create(CreateCompanyRequest("Frunza Company", "frunza.com")))
          companyOpt <- service(_.getBySlug(company.slug))
        } yield (company, companyOpt)

        program.assert {
          case (company, Some(companyRes)) =>
            company.name == "Frunza Company" &&
              company.url == "frunza.com" &&
              company.slug == "frunza-company" &&
              company == companyRes
          case _ => false
        }
      },

      test("get all") {
        val program = for {
          company <- service(_.create(CreateCompanyRequest("Frunza Company", "frunza.com")))
          company2 <- service(_.create(CreateCompanyRequest("Google", "google.com")))
          companies <- service(_.getAll)
        } yield (company, company2, companies)

        program.assert {
          case (company, company2, companies) =>
            companies.toSet == Set(company, company2)
        }
      }
    ).provide(CompanyServiceLive.layer, stubRepoLayer)
}
