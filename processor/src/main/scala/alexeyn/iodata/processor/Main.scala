package alexeyn.iodata.processor

import alexeyn.iodata.processor.Config._
import cats.data.NonEmptyList
import cats.effect.{IO, _}
import cats.implicits._
import com.typesafe.scalalogging.StrictLogging
import fs2.kafka.{AutoOffsetReset, _}
import fs2.{kafka, _}
import org.apache.http.HttpHost
import org.apache.kafka.common.serialization.StringDeserializer
import org.elasticsearch.client.{RestClient, RestHighLevelClient}

import scala.concurrent.ExecutionContext
import scala.language.higherKinds

object Main extends IOApp with StrictLogging {

  override def run(args: List[String]): IO[ExitCode] = {
    val stream = for {
      es <- EsClient.create[IO]
      kc <- KafkaConsumer.create[IO]
      _ <- kc.stream
        .mapAsync(KafkaMaxConcurrent) { msg =>
          Processor.processRecord[IO](msg.record.value, es).map(_ => msg)
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
      c <- consumerStream[F].using(consumerSettings(executionContext))
      _ <- fs2.Stream.eval(c.subscribe(topics))
    } yield c
}

object EsClient {

  def create[F[_]](implicit F: Sync[F]): Stream[F, RestHighLevelClient] = {
    val hosts = List(new HttpHost(EsHost, EsPort))
    Stream.bracket(F.delay(new RestHighLevelClient(RestClient.builder(hosts: _*))))(c => F.delay(c.close()))
  }
}
