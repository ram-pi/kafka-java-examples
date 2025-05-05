package com.github.prametta.producer;

import com.github.javafaker.Faker;
import io.confluent.common.utils.Utils;
import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.CreateTopicsResult;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.producer.*;
import org.apache.kafka.common.KafkaException;
import org.apache.kafka.common.errors.AuthorizationException;
import org.apache.kafka.common.errors.OutOfOrderSequenceException;
import org.apache.kafka.common.errors.ProducerFencedException;
import org.apache.kafka.common.errors.TopicExistsException;

import java.util.Collections;
import java.util.Properties;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

@Log4j2
public class MyTxnProducerWithDLQ implements Callback {

    private static final String DLQ_TOPIC = "dlq-topic";
    private static KafkaProducer<String, String> dlqProducer;

    @SneakyThrows
    public static void main(String[] args) {
        log.info("Producer with DLQ Running!");
        String topic = "test";

        // set properties
        Properties props = Utils.loadProps("client.properties");
        props.setProperty(ProducerConfig.LINGER_MS_CONFIG, String.valueOf(0));
        props.put(ProducerConfig.TRANSACTIONAL_ID_CONFIG, "my-transactional-id");

        // print properties
        log.info("Properties: {}", props);

        // Create DLQ topic if it doesn't exist
        try (AdminClient adminClient = AdminClient.create(props)) {
            NewTopic newTopic = new NewTopic(DLQ_TOPIC, 1, (short) 1);
            CreateTopicsResult result = adminClient.createTopics(Collections.singleton(newTopic));

            try {
                result.all().get(30, TimeUnit.SECONDS);
                log.info("DLQ topic '{}' created successfully", DLQ_TOPIC);
            } catch (ExecutionException e) {
                if (e.getCause() instanceof TopicExistsException) {
                    log.info("DLQ topic '{}' already exists", DLQ_TOPIC);
                } else {
                    log.error("Failed to create DLQ topic: {}", e.getMessage());
                    throw e;
                }
            }
        }

        // create the main producer
        KafkaProducer<String, String> producer = new KafkaProducer<>(props);

        // create DLQ producer
        Properties dlqProps = Utils.loadProps("client.properties");
        dlqProps.remove(ProducerConfig.TRANSACTIONAL_ID_CONFIG);
        dlqProducer = new KafkaProducer<>(dlqProps);

        // intercept shutdown
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            log.info("Closing producers!");
            producer.close();
            dlqProducer.close();
        }));

        // init transaction
        producer.initTransactions();

        // send data - asynchronous
        Faker faker = new Faker();
        String key = faker.letterify("????");
        String value = faker.letterify("????");
        try {
            log.info("Starting transaction");
            producer.beginTransaction();
            producer.send(
                            new ProducerRecord<>(
                                    topic,
                                    key,
                                    "Start transaction"))
                    .get(5, TimeUnit.SECONDS);
            producer.send(
                            new ProducerRecord<>(
                                    topic,
                                    key,
                                    value))
                    .get(5, TimeUnit.SECONDS);
            producer.send(
                            new ProducerRecord<>(
                                    topic,
                                    key,
                                    "End transaction"))
                    .get(5, TimeUnit.SECONDS);
            // Force txn expiration
            // set KAFKA_TRANSACTION_MAX_TIMEOUT_MS: 60000 in docker-compose.yml
            Thread.sleep(61000);
            producer.commitTransaction();
            log.info("Transaction commetted successfully");
        } catch (ProducerFencedException | OutOfOrderSequenceException | AuthorizationException e) {
            // These exceptions cannot be recovered from, so you should abort the
            // transaction and close the producer.
            // try - catch for abort the producer
            try {
                log.error("Fatal error, aborting transaction: {}", e.getMessage());
                producer.abortTransaction();
            } catch (Exception abortException) {
                log.error("Failed to abort transaction: {}", abortException.getMessage());
            }

            // Send failed message to DLQ
            log.error("Failed to send message to main topic, sending to DLQ: {}", e.getMessage());
            try {
                dlqProducer.send(
                                new ProducerRecord<>(
                                        DLQ_TOPIC,
                                        key,
                                        value + " | Error: " + e.getMessage()))
                        .get(5, TimeUnit.SECONDS);
                log.info("Successfully sent failed message to DLQ");
            } catch (Exception dlqException) {
                log.error("Failed to send message to DLQ: {}", dlqException.getMessage());
            }

            System.exit(1);
        } catch (KafkaException e) {
            // For all other recoverable exceptions, you can abort the transaction and
            // continue processing.
            log.error("Unable to send the message: {}", e.getMessage());
            log.info("Aborting transaction");
            producer.abortTransaction();
        }
        // Sleep 1 second
        Thread.sleep(10000);
    }

    @Override
    public void onCompletion(RecordMetadata recordMetadata, Exception e) {
        if (e != null) {
            log.error("Unable to send the message: {}", e.getMessage());
            return;
        }
        log.info("Message sent to topic: {}, on partition: {}, with offset: {}", recordMetadata.topic(),
                recordMetadata.partition(), recordMetadata.offset());
    }
}