package alexeyn.iotdata

import java.util.UUID

import alexeyn.iotdata.Config._
import cats.{Applicative, MonadError}
import cats.effect.{ExitCode, IO, IOApp, _}
import cats.implicits._
import com.typesafe.scalalogging.StrictLogging
import cron4s.Cron
import eu.timepit.fs2cron.schedule
import fs2.Stream
import fs2.kafka._

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

object Task extends StrictLogging {

  private def sample[F[_]](id: UUID)(implicit M: MonadError[F, Throwable]): F[Sample] =
    M.fromOption(SampleGen
      .generate(id, System.currentTimeMillis())
      .sample,
      new RuntimeException(s"Failed to generate a sample for $id"))

  def produceSample[F[_] : Applicative](id: UUID, p: KafkaProducer[F, String, String])
                                       (implicit M: MonadError[F, Throwable]): Stream[F, Unit] = for {
    s <- Stream.eval(sample[F](id))
    _ <- Stream.eval(Applicative[F].pure(logger.info(s"$s")))

    record = ProducerRecord(Config.KafkaTopic, s.deviceId.toString + s.time.toString, s.toString)
    message = ProducerMessage.one(record)

    _ <- Stream.eval(p.produce(message))
  } yield ()
}

object KafkaProducer {
  private lazy val producerSettings = ProducerSettings[String, String]
    .withBootstrapServers(KafkaBootstrapServer)

  def create[F[_] : ConcurrentEffect]: Stream[F, KafkaProducer[F, String, String]] =
    producerStream[F].using(producerSettings)
}
