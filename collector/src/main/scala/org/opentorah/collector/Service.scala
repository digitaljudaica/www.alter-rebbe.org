package org.opentorah.collector

import java.util.concurrent.Executors

import cats.effect.{Blocker, ExitCode}
import org.http4s.{HttpRoutes, StaticFile}
//import org.http4s.{Charset, Response}
import org.http4s.dsl.Http4sDsl
//import org.http4s.headers.`Content-Type`
//import org.http4s.MediaType
//import org.http4s.dsl.io._   // GET, Root, Ok
import org.http4s.implicits._  // orNotFound :)
import org.http4s.server.blaze._
import zio.{App, Task, URIO, ZEnv, ZIO}
import zio.interop.catz._
import zio.interop.catz.implicits._

object Service extends App {

  private val blocker: Blocker = Blocker.liftExecutorService(Executors.newFixedThreadPool(2))

  private val staticResourceExtensions: Seq[String] = Seq(".ico", ".css", ".js", ".jpeg")

  private val dsl = Http4sDsl[Task]
  import dsl._

  private val service = HttpRoutes.of[Task] {
    case request @ GET -> Root / path if staticResourceExtensions.exists(path.endsWith) =>
      StaticFile.fromResource("/" + path, blocker, Some(request))
        .getOrElseF(NotFound("Not found!!!"))

    //    case GET -> Root => Ok("hello1!")
  }

  //  def renderHtml(content: String): IO[Response[IO]] = Ok(content).map(
  //    _.withContentType(`Content-Type`(MediaType.`text`.`html`, Charset.`UTF-8`))
  //  )

  // To be accessible when running in a docker container the server must bind to all IPs, not just 127.0.0.1:
  val server: ZIO[ZEnv, Throwable, Unit] = ZIO.runtime[ZEnv].flatMap { implicit rts =>
    BlazeServerBuilder[Task]
      .bindHttp(host = "0.0.0.0", port = getServicePort)
      .withWebSockets(false)
      .withHttpApp(service.orNotFound)
      .serve
      .compile[Task, Task, ExitCode]
      .drain
  }

  private def getServicePort: Int =
    scala.util.Properties.envOrNone("SERVICE_PORT").map(_.toInt).getOrElse(8090)

  def run(args: List[String]): URIO[ZEnv, Int] =
    server.fold(_ => 1, _ => 0)
}
