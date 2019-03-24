## IoT Data Processing

### Simulator

Generates data samples and sends them to Kafka topic

### Processor

Consumes Kafka topic and stores records as is, i.e. in JSON format
into Elasticsearch index `samples`

### Run 

#### Prerequisites

- JDK 1.8
- SBT
- Docker
- Docker-Compose

#### Start environment 

To start development environment run:

```bash
sh start.sh
```

It will start:
 - single node Kafka instance together with single node ZooKeeper instance.
 - 2 nodes of Elasticsearch cluster

To stop development environment run:

```bash
sh stop.sh
```

#### Run Application

To start data simulator run:

```bash
sbt "simulator/run"
```

To start data processor run:

```bash
sbt "processor/run"
```
