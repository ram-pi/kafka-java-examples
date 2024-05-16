package com.github.prametta.streams;

import com.github.javafaker.Faker;
import com.github.prametta.model.ZeldaOuterClass;
import io.confluent.common.utils.Utils;
import io.confluent.kafka.serializers.protobuf.KafkaProtobufDeserializerConfig;
import io.confluent.kafka.streams.serdes.protobuf.KafkaProtobufSerde;
import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.StreamsConfig;
import org.apache.kafka.streams.Topology;
import org.apache.kafka.streams.errors.StreamsUncaughtExceptionHandler;
import org.apache.kafka.streams.kstream.Consumed;
import org.apache.kafka.streams.kstream.KStream;
import org.apache.kafka.streams.kstream.KTable;
import org.apache.kafka.streams.kstream.Produced;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Log4j2
public class MyKafkaZeldaStreamApp implements Runnable, KafkaStreams.StateListener, StreamsUncaughtExceptionHandler {

    static ExecutorService executor;

    static String inputTopic = "zelda";

    @SneakyThrows
    public static void main(String[] args) {
        executor = Executors.newFixedThreadPool(10);
        executor.execute(new MyKafkaZeldaStreamApp());
    }

    protected KafkaProtobufSerde<ZeldaOuterClass.Zelda> zeldaProtobufSerde(Properties props) {
        final KafkaProtobufSerde<ZeldaOuterClass.Zelda> protobufSerde = new KafkaProtobufSerde<>();
        Map<String, String> serdeConfig = new HashMap<>();
        serdeConfig.putAll((Map) props);
        serdeConfig.put(KafkaProtobufDeserializerConfig.SPECIFIC_PROTOBUF_VALUE_TYPE, ZeldaOuterClass.Zelda.class.getName());
        protobufSerde.configure(serdeConfig, false);
        return protobufSerde;
    }

    /**
     * |
     * Run the Kafka Streams application.
     */
    @Override
    @SneakyThrows
    public void run() {
        log.info("MyProtoBufKafkaStreamApp Running!");

        // Create Faker to generate random data
        Faker faker = new Faker();

        // Set up configuration properties for the Kafka Streams application
        Properties props = Utils.loadProps("client.properties");
        props.put(StreamsConfig.APPLICATION_ID_CONFIG, "my-kafka-streams-app-" + faker.numerify("###"));
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        props.put(StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG, Serdes.String().getClass().getName());
        props.put(StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG, Serdes.String().getClass().getName());

        // Create a StreamsBuilder object to build the topology of the Kafka Streams application
        StreamsBuilder builder = new StreamsBuilder();

        // Create a source stream
        KStream<String, ZeldaOuterClass.Zelda> source = builder.stream(inputTopic, Consumed.with(Serdes.String(), zeldaProtobufSerde(props)));

        // group by key and count the values
        KTable<String, Long> counts = source
                .groupByKey()
                .count();

        // Print the key-value pairs
        counts.toStream().foreach((key, value) -> {
            log.info("Key: {}, Count: {}", key, value);
        });

        // send the key-value pairs to a Kafka topic
        counts.toStream().to("zelda.characters.per.game", Produced.with(Serdes.String(), Serdes.Long()));

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
