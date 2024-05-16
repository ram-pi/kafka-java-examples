package com.github.prametta.streams;

import com.github.javafaker.Faker;
import io.confluent.common.utils.Utils;
import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.StreamsConfig;
import org.apache.kafka.streams.Topology;
import org.apache.kafka.streams.errors.StreamsUncaughtExceptionHandler;
import org.apache.kafka.streams.kstream.KStream;

import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Log4j2
public class MyKafkaStreamInteractiveQueryApp implements Runnable, KafkaStreams.StateListener, StreamsUncaughtExceptionHandler {

    static ExecutorService executor;

    static String inputTopic = "quotes";

    @SneakyThrows
    public static void main(String[] args) {
        executor = Executors.newFixedThreadPool(10);
        executor.execute(new MyKafkaStreamInteractiveQueryApp());
    }

    @Override
    @SneakyThrows
    public void run() {
        log.info("MyKafkaStreamApp Running!");

        // Create Faker
        Faker faker = new Faker();

        // Set up the configuration properties for the Kafka Streams application
        Properties props = Utils.loadProps("client.properties");
        props.put(StreamsConfig.APPLICATION_ID_CONFIG, "my-kafka-streams-app-" + faker.numerify("###"));
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        props.put(StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG, Serdes.String().getClass().getName());
        props.put(StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG, Serdes.String().getClass().getName());
        // Create a StreamsBuilder object to build the topology of the Kafka Streams
        // application
        StreamsBuilder builder = new StreamsBuilder();

        // source stream
        KStream<String, String> source = builder.stream(inputTopic);

        source.peek((k, v) -> {
            log.info("Key: {}, Value: {}", k, v);
        });

        // Build the topology of the Kafka Streams application
        Topology topology = builder.build();

        // Print the topology of the Kafka Streams application to the console
        log.info("{}", topology.describe());

        // Create a KafkaStreams object to start the Kafka Streams application
        KafkaStreams streams = new KafkaStreams(topology, props);

        // Set the state listener for the Kafka Streams application
        streams.setStateListener(this);

        // Set the uncaught exception handler for the Kafka Streams application
        streams.setUncaughtExceptionHandler(this);

        // Stop the Kafka Streams application gracefully when the JVM is shut down
        Runtime.getRuntime().addShutdownHook(new Thread(streams::close));

        // Start the Kafka Streams application
        streams.cleanUp();
        streams.start();
    }

    @Override
    public void onChange(KafkaStreams.State newState, KafkaStreams.State oldState) {
        log.info("State changed from: {} to: {}", oldState.name(), newState.name());
    }

    @Override
    public StreamThreadExceptionResponse handle(Throwable exception) {
        log.error("Uncaught exception", exception);
        return null;
    }
}
