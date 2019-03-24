package alexeyn.iodata.processor

//TODO: this must be a part of configuration file
object Config {
  val KafkaTopic = "IoT-Data"
  val KafkaBootstrapServer = "localhost:9092"
  val KafkaConsumerGroupId = "group"
  val KafkaMaxConcurrent = 25
  val EsHost = "localhost"
  val EsPort = 9200
  val EsIndex = "samples"
}
