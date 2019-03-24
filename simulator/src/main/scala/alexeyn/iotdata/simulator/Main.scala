package alexeyn.iotdata.simulator

import java.util.UUID

import Config._
import cats.{Applicative, MonadError}
import cats.effect.{ExitCode, IO, IOApp, _}
import cats.implicits._
import com.typesafe.scalalogging.StrictLogging
import cron4s.Cron
import eu.timepit.fs2cron.schedule
import fs2.Stream
import fs2.kafka._
import upickle.default._
import scala.language.higherKinds

//TODO: this must be a part of configuration file
object Config {
  val KafkaTopic = "IoT-Data"
  val KafkaBootstrapServer = "localhost:9092"
  val NumberOfDevices = 3
  val CronExpression = "*/1 * * ? * *"
}

object Main extends IOApp {

  override def run(args: List[String]): IO[ExitCode] = {
    val scheduledTasks = for {
      cronExp <- Stream.eval(IO(Cron.unsafeParse(CronExpression)))
      p <- KafkaProducer.create[IO]
      devices = List.fill(NumberOfDevices)(UUID.randomUUID())
      s <- schedule(devices.map(id => cronExp -> Task.produceSample(id, p)))
    } yield s

    scheduledTasks.compile.drain.as(ExitCode.Success)
  }
}

object Task extends StrictLogging with Codecs {

  private def data[F[_]](id: UUID)(implicit M: MonadError[F, Throwable]) =
    M.fromOption(
      DataGen
        .generate(id, System.currentTimeMillis())
        .sample,
      new RuntimeException(s"Failed to generate a data for $id")
    )

  private def createMessage(d: Data) = {
    val key = d.data.deviceId.toString + d.data.time.toString
    val record = ProducerRecord(Config.KafkaTopic, key, write(d))
    ProducerMessage.one(record)
  }

  def produceSample[F[_]: Applicative](id: UUID, p: KafkaProducer[F, String, String])(
    implicit M: MonadError[F, Throwable]
  ): Stream[F, Unit] =
    for {
      d <- Stream.eval(data[F](id))
      _ <- Stream.eval(Applicative[F].pure(logger.info(s"$d")))
      _ <- Stream.eval(p.produce(createMessage(d)))
    } yield ()
}

object KafkaProducer {
  private lazy val producerSettings = ProducerSettings[String, String]
    .withBootstrapServers(KafkaBootstrapServer)

  def create[F[_]: ConcurrentEffect]: Stream[F, KafkaProducer[F, String, String]] =
    producerStream[F].using(producerSettings)
}
