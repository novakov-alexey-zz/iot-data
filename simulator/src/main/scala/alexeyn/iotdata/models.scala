package alexeyn.iotdata

import java.util.UUID

case class Data(data: Sample)

case class Sample(deviceId: UUID, temperature: Int, location: Location, time: Long)

case class Location(latitude: Float, longitude: Float)