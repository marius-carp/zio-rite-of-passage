package scala.com.frunza.reviewboard.http.controllers

import com.frunza.reviewboard.domain.data.Company
import com.frunza.reviewboard.http.controllers.CompanyController
import com.frunza.reviewboard.http.requests.CreateCompanyRequest
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

  private def backendStubZIO(endpointFun: CompanyController => ServerEndpoint[Any, Task]) = for {
    controller <- CompanyController.makeZio
    backendStub <- ZIO.succeed(TapirStubInterpreter(SttpBackendStub(MonadError[Task]))
      .whenServerEndpointRunLogic(endpointFun(controller))
      .backend()
    )
  } yield backendStub

  override def spec: Spec[TestEnvironment with Scope, Any] =
    suite("CompanyControllerSpec")(
      test("post company") {
        val program = for {
          backendStub <- backendStubZIO(_.create)
          response <- basicRequest
            .post(uri"/companies")
            .body(CreateCompanyRequest("Frunza Company", "frunza.com").toJson)
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
            .contains(List.empty)
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
            .isEmpty
        }
      }
    )


}
