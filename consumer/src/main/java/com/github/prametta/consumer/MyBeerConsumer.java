package com.github.prametta.consumer;

import com.github.javafaker.Faker;
import com.github.prametta.model.Beer;
import io.confluent.common.utils.Utils;
import io.confluent.kafka.serializers.KafkaJsonDeserializerConfig;
import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;
import org.apache.kafka.clients.consumer.*;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.errors.WakeupException;

import java.time.Duration;
import java.util.Collection;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * This class represents a Kafka consumer for the "beers" topic. It implements
 * the Runnable and ConsumerRebalanceListener interfaces.
 * It creates a Kafka consumer with the given properties and subscribes to the
 * "beers" topic.
 * It polls for new data and logs the received messages.
 * It also implements a graceful shutdown mechanism by adding a shutdown hook to
 * the runtime.
 */
@Log4j2
public class MyBeerConsumer implements Runnable, ConsumerRebalanceListener {

    static ExecutorService executor;
    Consumer<String, Beer> consumer;

    public static void main(String[] args) {
        executor = Executors.newFixedThreadPool(10);
        executor.execute(new MyBeerConsumer());

    }

    @Override
    public void onPartitionsRevoked(Collection<TopicPartition> partitions) {
        log.info("Partitions revoked: {}", partitions);
    }

    @Override
    public void onPartitionsAssigned(Collection<TopicPartition> partitions) {
        log.info("Partitions assigned: {}", partitions);
    }

    @Override
    @SneakyThrows
    public void run() {
        log.info("MyBeerConsumer Running!");

        // graceful shutdown
        // get a reference to the current thread
        final Thread mainThread = Thread.currentThread();
        log.info("Current thread: {}", mainThread);
        // adding the shutdown hook
        Runtime.getRuntime().addShutdownHook(new Thread() {
            public void run() {
                log.info("Detected a shutdown, let's exit by calling consumer.wakeup()...");
                consumer.wakeup();
                log.info("Waiting for the main thread to finish...");
                executor.shutdown();

                // join the main thread to allow the execution of the code in the main thread
                try {
                    mainThread.join();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        });

        // topic
        String topic = "beers";

        // create faker
        Faker faker = new Faker();

        // set properties
        Properties props = Utils.loadProps("client.properties");
        props.setProperty(ConsumerConfig.GROUP_ID_CONFIG, faker.numerify("group-###")); // group-123
        props.setProperty(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        props.setProperty(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG,
                "org.apache.kafka.common.serialization.StringDeserializer");
        props.setProperty(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG,
                "io.confluent.kafka.serializers.json.KafkaJsonSchemaDeserializer");
        props.setProperty(KafkaJsonDeserializerConfig.JSON_VALUE_TYPE, Beer.class.getName());

        // print properties
        log.info("Properties: {}", props);

        // create the consumer
        consumer = new KafkaConsumer<>(props);

        // subscribe to topic
        consumer.subscribe(List.of(topic), this);

        // poll for new data
        try {
            while (true) {
                ConsumerRecords<String, Beer> records = consumer.poll(Duration.ofMillis(100));

                records.forEach(r -> log.info("Received message key = [{}], value = [{}], offset = [{}]", r.key(),
                        r.value(), r.offset()));
            }
        } catch (WakeupException e) {
            log.info("Received shutdown signal!");
        } finally {
            close();
        }
    }

    public void close() {
        if (consumer == null)
            return;

        log.info("Closing consumer ...");
        consumer.close();
    }

}
