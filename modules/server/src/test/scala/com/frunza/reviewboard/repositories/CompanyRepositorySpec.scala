package scala.com.frunza.reviewboard.repositories


import org.testcontainers.containers.PostgreSQLContainer
import com.frunza.reviewboard.domain.data.Company
import com.frunza.reviewboard.repositories.{CompanyRepository, CompanyRepositoryLive, Repository}

import zio.test.*
import zio.*
import java.sql.SQLException

import scala.com.frunza.reviewboard.syntax.*

object CompanyRepositorySpec extends ZIOSpecDefault with RepositorySpec {

  val company = Company(1L, "frunza-company", "Frunza Company", "frunza.com")
  private def genCompany = {
    Company(1L, getString, getString, getString)
  }

  private def getString: String = {
    scala.util.Random.alphanumeric.take(8).mkString
  }

  override val initScript: String = "sql/companies.sql"

  override def spec: Spec[TestEnvironment with Scope, Any] = {
    suite("CompanyRepositorySpec")(
      test("create a company") {
        val program = for {
          repo <- ZIO.service[CompanyRepository]
          company <- repo.create(company)
        } yield company

        program.assert {
          case Company(_, "frunza-company", "Frunza Company", "frunza.com", _, _, _, _, _) => true
          case _ => false
        }
      },
      test("create a duplicate company should throw error") {
        val program = for {
          repo <- ZIO.service[CompanyRepository]
          _ <- repo.create(company)
          error <- repo.create(company).flip
        } yield error

        program.assert(_.isInstanceOf[SQLException])
      },
      test("get by id and slug") {
        val program: ZIO[CompanyRepository, Throwable, (Company, Option[Company], Option[Company])] = for {
          repo <- ZIO.service[CompanyRepository]
          _ <- repo.create(company)
          fetchById <- repo.getById(company.id)
          fetchBySlug <- repo.getBySlug(company.slug)
        } yield (company, fetchById, fetchBySlug)

        program.assert {
          case (company: Company, fetchById, fetchBySlug) =>
            fetchById.contains(company) && fetchBySlug.contains(company)
        }
      },
      test("update record") {
        val program: ZIO[CompanyRepository, Throwable, (Option[Company], Company)] = for {
          repo <- ZIO.service[CompanyRepository]
          comp <- repo.create(company)
          updated <- repo.update(company.id, _.copy(url = "blog.frunza.com"))
          fetchedById <- repo.getById(comp.id)
        } yield (fetchedById, updated)

        program.assert {
          case (fetchedById, updated: Company) =>
            fetchedById.contains(updated)
        }
      },
      test("delete record") {
        val program: ZIO[CompanyRepository, Throwable, Option[Company]] = for {
          repo <- ZIO.service[CompanyRepository]
          comp <- repo.create(company)
          _ <- repo.delete(company.id)
          fetchedById <- repo.getById(company.id)
        } yield fetchedById

        program.assert(_.isEmpty)
      },
      test("get all") {
        val program: ZIO[CompanyRepository, Throwable, (IndexedSeq[Company], List[Company])] = for {
          repo <- ZIO.service[CompanyRepository]
          companies <- ZIO.collectAll((1 to 10).map(_ => repo.create(genCompany)))
          companiesFetched <- repo.get
        } yield (companies, companiesFetched)

        program.assert {
          case (companies, companiesFetched) =>
            companies.toSet == companiesFetched.toSet
        }
      }
    ).provide(
      CompanyRepositoryLive.layer,
      dataSourceLayer,
      Repository.quillLayer,
      Scope.default)
  }
}
