package com.github.prametta.consumer;


import com.github.javafaker.Faker;
import io.confluent.common.utils.Utils;
import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;
import org.apache.kafka.clients.consumer.*;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.errors.WakeupException;

import java.time.Duration;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Log4j2
public class MyConsumerWithAssign implements Runnable, ConsumerRebalanceListener {

    static ExecutorService executor;
    static String TOPIC = "test";

    static Integer POLL_TIMEOUT = 50000;
    Consumer<String, String> consumer;

    public static void main(String[] args) {
        executor = Executors.newFixedThreadPool(1);
        executor.execute(new MyConsumerWithAssign());

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
        log.info("MyConsumer Running!");

        // graceful shutdown
        // get a reference to the current thread
        final Thread mainThread = Thread.currentThread();
        log.info("Current thread: {}", mainThread);
        // log TOPIC and POLL_TIMEOUT
        log.info("TOPIC: {}", TOPIC);
        log.info("POLL_TIMEOUT: {}", POLL_TIMEOUT);

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
        String topic = TOPIC;

        // create faker
        Faker faker = new Faker();

        // set properties
        Properties props = Utils.loadProps("client.properties");
        props.setProperty(ConsumerConfig.GROUP_ID_CONFIG, faker.numerify("group-###")); // group-123
        props.setProperty(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "latest");
        props.setProperty(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "false");

        // print properties
        log.info("Properties: {}", props);

        // create the consumer
        consumer = new KafkaConsumer<>(props);

        TopicPartition tp = new TopicPartition(topic, 0);
        consumer.assign(List.of(tp));
        consumer.seekToBeginning(List.of(tp));

        while (true) {
            ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(100));
            for (ConsumerRecord<String, String> record : records) {
                System.out.printf("offset = %d, key = %s, value = %s%n", record.offset(), record.key(), record.value());
            }
        }
    }

    public void close() {
        if (consumer == null)
            return;

        log.info("Closing consumer ...");
        consumer.close();
    }
}

