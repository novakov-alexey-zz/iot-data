package alexeyn.iotdata

import java.util.UUID

case class Sample(deviceId: UUID, temperature: Int, location: Location, time: Long)

case class Location(lat: Float, long: Float)