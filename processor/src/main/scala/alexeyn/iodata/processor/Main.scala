package alexeyn.iodata.processor

import alexeyn.iodata.processor.Config._
import cats.data.NonEmptyList
import cats.effect._
import cats.implicits._
import com.typesafe.scalalogging.StrictLogging
import fs2.kafka
import fs2.kafka.{AutoOffsetReset, _}
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.common.serialization.StringDeserializer

import scala.concurrent.ExecutionContext

object Config {
  val KafkaTopic = "IoT-Data"
  val KafkaBootstrapServer = "localhost:9092"
  val KafkaConsumerGroupId = "group"
  val KafkaMaxConcurrent = 25
}

object Main extends IOApp with StrictLogging {

  override def run(args: List[String]): IO[ExitCode] = {

    def processRecord(record: ConsumerRecord[String, String]): IO[Unit] = IO {
      logger.info(s"${record.key} -> ${record.value}")
      ()
    }

    val stream = for {
      c <- KafkaConsumer.create[IO]
      _ <- c.stream
        .mapAsync(KafkaMaxConcurrent) { msg =>
          processRecord(msg.record).map(_ => msg)
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
