# Kafka-Examples

## Producing messages (GOT quotes) - 100 messages each 60 seconds

```bash
mvn install 
mvn exec:java -pl producer -Dexec.mainClass=io.confluent.prametta.producer.MyProducer
```

## Consuming messages (GOT quotes)

```bash
mvn install
mvn exec:java -pl consumer -Dexec.mainClass=com.github.prametta.consumer.MyConsumer
```

## Running Migrate ACLs Util

Create a file `source_client.properties` and `dest_client.properties` with the following content:

```
bootstrap.servers=...
sasl.jaas.config=...
sasl.mechanism=...
security.protocol=...
```

And run the following command:

```bash
mvn=install
java=-cp java -cp utils/target/utils-1.0-SNAPSHOT.jar com.github.prametta.utils.MigrateACLs source_client.properties
dest_client.properties
```