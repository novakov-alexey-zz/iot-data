package alexeyn.iotdata.simulator

import upickle.default._

trait Codecs {

  implicit def rLocation: Writer[Location] = macroW

  implicit def rSample: Writer[Sample] = macroW

  implicit def rData: Writer[Data] = macroW

}
