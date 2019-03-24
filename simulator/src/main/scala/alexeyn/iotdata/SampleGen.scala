package alexeyn.iotdata

import java.util.UUID

import org.scalacheck.Gen

object SampleGen {
  private val lat = Gen.choose(1, 100f)
  private val long = Gen.choose(1, 100f)
  private val temp = Gen.choose(-100, 100)


  def generate(deviceId: UUID, sysTime: Long)(): Gen[Sample] = for {
    t <- temp
    la <- lat
    lo <- long
  } yield Sample(deviceId, t, Location(la, lo), sysTime)

}
