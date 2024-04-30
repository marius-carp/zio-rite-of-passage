package scala.com.frunza.reviewboard.http.controllers

import com.frunza.reviewboard.domain.data.{Company, User, UserID, UserToken}
import com.frunza.reviewboard.http.controllers.CompanyController
import com.frunza.reviewboard.http.requests.CreateCompanyRequest
import com.frunza.reviewboard.services.{CompanyService, JWTService}
import sttp.client3.*
import sttp.client3.testing.SttpBackendStub
import sttp.monad.MonadError
import sttp.tapir.server.stub.TapirStubInterpreter
import sttp.tapir.ztapir.RIOMonadError
import zio.test.*
import zio.*
import zio.json.*
import sttp.tapir.server.ServerEndpoint

import scala.com.frunza.reviewboard.syntax.*

object CompanyControllerSpec extends ZIOSpecDefault {

  private given zioMonadError: MonadError[Task] = new RIOMonadError[Any]
  private val company = Company(1L, "frunza-company", "Frunza Company", "frunza.com")

  private val serviceStub = new CompanyService {
    override def create(req: CreateCompanyRequest): Task[Company] = ZIO.succeed(company)

    override def getAll: Task[List[Company]] = ZIO.succeed(List(company))

    override def getById(id: Long): Task[Option[Company]] = ZIO.succeed {
      if (id == 1) Some (company)
      else None
    }

    override def getBySlug(slug: String): Task[Option[Company]] = ZIO.succeed {
      if (slug == "frunza-company") Some(company)
      else None
    }
  }

  private def backendStubZIO(endpointFun: CompanyController => ServerEndpoint[Any, Task]) = for {
    controller <- CompanyController.makeZIO
    backendStub <- ZIO.succeed(TapirStubInterpreter(SttpBackendStub(MonadError[Task]))
      .whenServerEndpointRunLogic(endpointFun(controller))
      .backend()
    )
  } yield backendStub

  private val jwtServiceStub = new JWTService {
    override def createToken(user: User): Task[UserToken] =
      ZIO.succeed(UserToken(user.email, "Pass", 99999L))

    override def verifyToken(token: String): Task[UserID] =
      ZIO.succeed(UserID(1, "marius@frunza.com"))
  }

  override def spec: Spec[TestEnvironment with Scope, Any] =
    suite("CompanyControllerSpec")(
      test("post company") {
        val program = for {
          backendStub <- backendStubZIO(_.create)
          response <- basicRequest
            .post(uri"/companies")
            .body(CreateCompanyRequest("Frunza Company", "frunza.com").toJson)
            .header("Authorization", "Bearer Pass")
            .send(backendStub)
        } yield response.body

        program.assert { responseBody =>
          responseBody.toOption.flatMap(_.fromJson[Company].toOption)
            .contains(Company(1L, "frunza-company", "Frunza Company", "frunza.com"))
        }
      },

      test("get all") {
        val program = for {
          backendStub <- backendStubZIO(_.getAll)
          response <- basicRequest
            .get(uri"/companies")
            .send(backendStub)
        } yield response.body

        program.assert { responseBody =>
          responseBody.toOption
            .flatMap(_.fromJson[List[Company]].toOption)
            .contains(List(company))
        }
      },

      test("get by id") {
        val program = for {
          backendStub <- backendStubZIO(_.getById)
          response <- basicRequest
            .get(uri"/companies/1")
            .send(backendStub)
        } yield response.body

        program.assert { responseBody =>
          responseBody.toOption
            .flatMap(_.fromJson[Company].toOption)
            .contains(company)
        }
      }
    ).provide(
      ZLayer.succeed(serviceStub),
      ZLayer.succeed(jwtServiceStub)
    )


}
