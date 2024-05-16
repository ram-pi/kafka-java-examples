package com.github.prametta.consumer;

import java.time.Duration;
import java.util.Collection;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRebalanceListener;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.errors.WakeupException;

import com.github.javafaker.Faker;
import com.github.prametta.model.ZeldaOuterClass.Zelda;

import io.confluent.common.utils.Utils;
import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;

@Log4j2
public class MyZeldaConsumer implements Runnable, ConsumerRebalanceListener {

    static ExecutorService executor;
    Consumer<String, Zelda> consumer;

    public static void main(String[] args) {
        executor = Executors.newFixedThreadPool(10);
        executor.execute(new MyZeldaConsumer());

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
        log.info("MyZeldaConsumer Running!");

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
        String topic = "zelda";

        // create faker
        Faker faker = new Faker();

        // set properties
        Properties props = Utils.loadProps("client.properties");
        props.setProperty(ConsumerConfig.GROUP_ID_CONFIG, faker.numerify("group-###")); // group-123
        props.setProperty(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        props.setProperty(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG,
                "org.apache.kafka.common.serialization.StringDeserializer");
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG,
                "io.confluent.kafka.serializers.protobuf.KafkaProtobufDeserializer");

        // print properties
        log.info("Properties: {}", props);

        // create the consumer
        consumer = new KafkaConsumer<>(props);

        // subscribe to topic
        consumer.subscribe(List.of(topic), this);

        // poll for new data
        try {
            while (true) {
                ConsumerRecords<String, Zelda> records = consumer.poll(Duration.ofMillis(100));

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
