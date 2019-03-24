package alexeyn.iodata.processor

import alexeyn.iodata.processor.Config._
import cats.{Applicative, MonadError}
import cats.data.NonEmptyList
import cats.effect._
import cats.implicits._
import com.typesafe.scalalogging.StrictLogging
import fs2.kafka
import fs2.kafka.{AutoOffsetReset, _}
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.common.serialization.StringDeserializer
import fs2._
import org.apache.http.HttpHost
import cats.effect.IO
import org.elasticsearch.action.index.IndexRequest
import org.elasticsearch.client.{RequestOptions, RestClient, RestHighLevelClient}
import org.elasticsearch.common.xcontent.XContentType

import scala.concurrent.ExecutionContext
import scala.language.higherKinds

object Config {
  val KafkaTopic = "IoT-Data"
  val KafkaBootstrapServer = "localhost:9092"
  val KafkaConsumerGroupId = "group"
  val KafkaMaxConcurrent = 25
  val EsHost = "localhost"
  val EsPort = 9200
  val EsIndex = "samples"
}

object Main extends IOApp with StrictLogging {

  override def run(args: List[String]): IO[ExitCode] = {

    def processRecord[F[_]: Applicative](record: ConsumerRecord[String, String], client: RestHighLevelClient)(
      implicit M: MonadError[F, Throwable]
    ): F[Unit] =
      M.catchNonFatal {
        logger.info(s"${record.key} -> ${record.value}")
        val req = new IndexRequest(EsIndex, "doc").source(record.value, XContentType.JSON)
        val res = client.index(req, RequestOptions.DEFAULT)

        val failed = res.getShardInfo.getFailed > 0
        failed match {
          case true => M.raiseError(new RuntimeException(res.getShardInfo.getFailures.to[List].mkString(",")))
          case _ => M.pure(())
        }
      }

    val stream = for {
      es <- EsClient.create[IO]
      kc <- KafkaConsumer.create[IO]
      _ <- kc.stream
        .mapAsync(KafkaMaxConcurrent) { msg =>
          processRecord[IO](msg.record, es).map(_ => msg)
        }
        .map(_.committableOffset)
        .through(commitBatch)
    } yield ()

    stream.compile.drain.as(ExitCode.Success)
  }
}

object KafkaConsumer {
  private val consumerSettings = (executionContext: ExecutionContext) =>
    kafka
      .ConsumerSettings(
        keyDeserializer = new StringDeserializer,
        valueDeserializer = new StringDeserializer,
        executionContext = executionContext
      )
      .withAutoOffsetReset(AutoOffsetReset.Earliest)
      .withBootstrapServers(KafkaBootstrapServer)
      .withGroupId(KafkaConsumerGroupId)

  private val topics = NonEmptyList.one(KafkaTopic)

  def create[F[_]: ConcurrentEffect: ContextShift: Timer]: fs2.Stream[F, KafkaConsumer[F, String, String]] =
    for {
      executionContext <- consumerExecutionContextStream[F]
      consumer <- consumerStream[F].using(consumerSettings(executionContext))
      _ <- fs2.Stream.eval(consumer.subscribe(topics))
    } yield consumer
}

object EsClient {

  def create[F[_]](implicit F: Sync[F]): Stream[F, RestHighLevelClient] = {
    val hosts = List(new HttpHost(EsHost, EsPort))
    Stream.bracket(F.delay(new RestHighLevelClient(RestClient.builder(hosts: _*))))(c => F.delay(c.close()))
  }
}
