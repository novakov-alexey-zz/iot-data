package alexeyn.iodata.processor

import java.sql.Timestamp
import java.time.format.DateTimeFormatter

import alexeyn.iodata.processor.Config.EsIndex
import cats.MonadError
import com.typesafe.scalalogging.StrictLogging
import io.circe.Json
import io.circe.parser.parse
import org.elasticsearch.action.index.IndexRequest
import org.elasticsearch.client.{RequestOptions, RestHighLevelClient}
import org.elasticsearch.common.xcontent.XContentType

import scala.language.higherKinds

object Processor extends StrictLogging {
  private val formatter: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS")

  private def transform(value: String): Option[String] = {
    val cur = parse(value).getOrElse(Json.Null).hcursor
    cur
      .downField("data")
      .downField("time")
      .withFocus(_.mapString(s => formatter.format(new Timestamp(s.toLong).toLocalDateTime)))
      .top
      .map(_.toString())
  }

  def processRecord[F[_]](recordVal: String, client: RestHighLevelClient)(
    implicit M: MonadError[F, Throwable]
  ): F[Unit] =
    M.catchNonFatal {
      logger.info(s"$recordVal")
      val json = transform(recordVal)
      json match {
        case Some(j) =>
          val req = new IndexRequest(EsIndex, "doc").source(j, XContentType.JSON)
          val res = client.index(req, RequestOptions.DEFAULT)
          logger.info(res.toString)

          val failed = res.getShardInfo.getFailed > 0
          failed match {
            case true => M.raiseError(new RuntimeException(res.getShardInfo.getFailures.to[List].mkString(",")))
            case _ => M.pure(())
          }
        case None => M.raiseError(new RuntimeException(s"Failed to parse into json: $recordVal"))
      }
    }
}
