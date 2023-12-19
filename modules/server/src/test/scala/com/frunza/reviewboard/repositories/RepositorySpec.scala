package scala.com.frunza.reviewboard.repositories

import org.testcontainers.containers.PostgreSQLContainer
import zio.*
import org.postgresql.ds.PGSimpleDataSource
import javax.sql.DataSource

trait RepositorySpec {


  private def createContainer(): PostgreSQLContainer[Nothing] = {
    val container: PostgreSQLContainer[Nothing] =
      PostgreSQLContainer("postgres")
        .withInitScript("sql/companies.sql")
    container.start()

    container
  }

  private def createDataSource(container: PostgreSQLContainer[Nothing]): DataSource = {
    val dataSource = new PGSimpleDataSource
    dataSource.setURL(container.getJdbcUrl)
    dataSource.setUser(container.getUsername)
    dataSource.setPassword(container.getPassword)

    dataSource
  }

  val dataSourceLayer = ZLayer {
    for {
      container <- ZIO.acquireRelease(
        ZIO.attempt(createContainer())
      )(container => ZIO.attempt(container.start()).ignoreLogged)
      dataSource <- ZIO.attempt(createDataSource(container))
    } yield dataSource
  }

}
