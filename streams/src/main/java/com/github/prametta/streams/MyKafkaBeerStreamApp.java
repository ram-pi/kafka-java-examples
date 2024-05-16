package com.github.prametta.streams;

import com.github.javafaker.Faker;
import com.github.prametta.model.Beer;
import io.confluent.common.utils.Utils;
import io.confluent.kafka.serializers.KafkaJsonDeserializerConfig;
import io.confluent.kafka.streams.serdes.json.KafkaJsonSchemaSerde;
import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.*;
import org.apache.kafka.streams.errors.StreamsUncaughtExceptionHandler;
import org.apache.kafka.streams.kstream.*;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Log4j2
public class MyKafkaBeerStreamApp implements Runnable, KafkaStreams.StateListener, StreamsUncaughtExceptionHandler {

    static ExecutorService executor;

    static String inputTopic = "beers";

    @SneakyThrows
    public static void main(String[] args) {
        executor = Executors.newFixedThreadPool(10);
        executor.execute(new MyKafkaBeerStreamApp());
    }

    protected KafkaJsonSchemaSerde<Beer> beerJsonSchemaSerde(Properties props) {
        final KafkaJsonSchemaSerde<Beer> jsonSchemaSerde = new KafkaJsonSchemaSerde<>();
        Map<String, String> serdeConfig = new HashMap<>();
        serdeConfig.putAll((Map) props);
        serdeConfig.put(KafkaJsonDeserializerConfig.JSON_VALUE_TYPE, Beer.class.getName());
        jsonSchemaSerde.configure(serdeConfig, false);
        return jsonSchemaSerde;
    }

    /**
     * |
     * Run the Kafka Streams application.
     */
    @Override
    @SneakyThrows
    public void run() {
        log.info("MyKafkaStreamApp Running!");

        // Create Faker to generate random data
        Faker faker = new Faker();

        // Set up configuration properties for the Kafka Streams application
        Properties props = Utils.loadProps("client.properties");
        props.put(StreamsConfig.APPLICATION_ID_CONFIG, "my-kafka-streams-app-" + faker.numerify("###"));
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        props.put(StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG, Serdes.String().getClass().getName());
        props.put(StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG, Serdes.String().getClass().getName());
        // Use a temporary directory for storing state, which will be automatically
        // removed after the test.
//        props.put(StreamsConfig.STATE_DIR_CONFIG, TestUtils.tempDirectory().getAbsolutePath());

        // Create a StreamsBuilder object to build the topology of the Kafka Streams application
        StreamsBuilder builder = new StreamsBuilder();

        // Create a source stream
        KStream<String, Beer> source = builder.stream(inputTopic, Consumed.with(Serdes.String(), beerJsonSchemaSerde(props)));

        // beer counter per style
        source
                .selectKey((key, value) -> value.getStyle())
                .groupByKey()
                .count()
                .toStream()
                .to("beers.counter", Produced.with(Serdes.String(), Serdes.Long()));

        // beer counter per style with windowing
        Duration windowSize = Duration.ofSeconds(300);
        TimeWindows tumblingWindow = TimeWindows.ofSizeWithNoGrace(windowSize);
        source
                .selectKey((key, value) -> value.getMalt())
                .groupByKey()
                .windowedBy(tumblingWindow)
                .count(Materialized.with(Serdes.String(), Serdes.Long()))
                .filter((key, value) -> value > 5)
                .suppress(Suppressed.untilWindowCloses(Suppressed.BufferConfig.unbounded()))
                .toStream()
                .map((wk, value) -> new KeyValue<>(wk.key(), value))
                .peek((key, value) -> log.info("Beer Malt: {}, Requests: {}", key, value))
                .to("beers.counter.windowed", Produced.with(Serdes.String(), Serdes.Long()));

        // Build the topology of the Kafka Streams application
        Topology topology = builder.build();

        // Print the topology of the Kafka Streams application to the console
        log.info("{}", topology.describe());

        // Create a KafkaStreams object to start the Kafka Streams application
        KafkaStreams streams = new KafkaStreams(topology, props);

        // Set the state listener and uncaught exception handler for the Kafka Streams application
        streams.setStateListener(this);
        streams.setUncaughtExceptionHandler(this);

        // Stop the Kafka Streams application gracefully when the JVM is shut down
        Runtime.getRuntime().addShutdownHook(new Thread(streams::close));

        // Clean up the Kafka Streams application before starting
        streams.cleanUp();

        // Start the Kafka Streams application
        streams.start();
    }

    @Override
    public void onChange(KafkaStreams.State newState, KafkaStreams.State oldState) {
        log.info("State changed from: {} to: {}", oldState.name(), newState.name());
    }

    @Override
    public StreamThreadExceptionResponse handle(Throwable exception) {
        log.error("Uncaught exception", exception);
        return StreamThreadExceptionResponse.SHUTDOWN_APPLICATION;
    }
}
