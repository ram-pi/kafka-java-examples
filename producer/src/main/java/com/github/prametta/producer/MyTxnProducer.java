package com.github.prametta.producer;

import com.github.javafaker.Faker;
import io.confluent.common.utils.Utils;
import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;
import org.apache.kafka.clients.producer.*;
import org.apache.kafka.common.KafkaException;
import org.apache.kafka.common.errors.AuthorizationException;
import org.apache.kafka.common.errors.OutOfOrderSequenceException;
import org.apache.kafka.common.errors.ProducerFencedException;

import java.util.Properties;
import java.util.concurrent.TimeUnit;

@Log4j2
public class MyTxnProducer implements Callback {

    @SneakyThrows
    public static void main(String[] args) {
        log.info("Producer Running!");
        String topic = "test";

        // set properties
        Properties props = Utils.loadProps("client.properties");
        props.setProperty(ProducerConfig.LINGER_MS_CONFIG, String.valueOf(0));
        props.put(ProducerConfig.TRANSACTIONAL_ID_CONFIG, "my-transactional-id");

        // print properties
        log.info("Properties: {}", props);

        // create the producer
        KafkaProducer<String, String> producer = new KafkaProducer<>(props);

        // intercept shutdown
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            log.info("Closing producer!");
            producer.close();
        }));

        // init transaction
        producer.initTransactions();

        // send data - asynchronous
        Faker faker = new Faker();
        while (true) {
            try {
                log.info("Starting transaction");
                producer.beginTransaction();
                for (int i = 0; i < 10; i++) {
                    producer.send(
                            new ProducerRecord<>(
                                    topic,
                                    faker.letterify("????"),
                                    faker.letterify("????"))
                    ).get(5, TimeUnit.SECONDS);
                }

                log.info("Committing transaction");
                producer.commitTransaction();
            } catch (ProducerFencedException | OutOfOrderSequenceException | AuthorizationException e) {
                // These exceptions cannot be recovered from, so you should abort the transaction and close the producer.
                log.error("Unable to send the message: {}", e.getMessage());
                producer.close();
            } catch (KafkaException e) {
                // For all other recoverable exceptions, you can abort the transaction and continue processing.
                log.error("Unable to send the message: {}", e.getMessage());
                log.info("Aborting transaction");
                producer.abortTransaction();
            }
            // Sleep 1 second
            Thread.sleep(10000);
        }
    }

    @Override
    public void onCompletion(RecordMetadata recordMetadata, Exception e) {
        if (e != null) {
            log.error("Unable to send the message: {}", e.getMessage());
            return;
        }
        log.info("Message sent to topic: {}, on partition: {}, with offset: {}", recordMetadata.topic(), recordMetadata.partition(), recordMetadata.offset());
    }
}
