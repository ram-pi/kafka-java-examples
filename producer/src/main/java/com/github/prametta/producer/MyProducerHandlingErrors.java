package com.github.prametta.producer;

import com.github.javafaker.Faker;
import io.confluent.common.utils.Utils;
import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;
import org.apache.kafka.clients.producer.*;
import org.apache.kafka.common.utils.Time;

import java.util.Properties;
import java.util.concurrent.TimeUnit;

@Log4j2
public class MyProducerHandlingErrors implements Callback {

    @SneakyThrows
    public static void main(String[] args) {
        log.info("Producer Running!");
        String topic = "test";

        // set properties
        Properties props = Utils.loadProps("client.properties");
        props.setProperty(ProducerConfig.LINGER_MS_CONFIG, String.valueOf(0));

        // print properties
        log.info("Properties: {}", props);

        // create the producer
        KafkaProducer<String, String> producer = new KafkaProducer<>(props);

        // intercept shutdown
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            log.info("Closing producer!");
            producer.close();
        }));

        // send data - asynchronous
        Faker faker = new Faker();
        while (true) {
            try {
                // get current time in millis
                long currentTimeMillis = System.currentTimeMillis();
                Time time = Time.SYSTEM;
                long nowMs = time.milliseconds();

                RecordMetadata rm = producer.send(
                        new ProducerRecord<>(
                                topic,
                                faker.letterify("????"),
                                faker.letterify("????"))
                ).get(5, TimeUnit.SECONDS);

                log.info(
                        "Message sent to topic: {}, on partition: {}, with offset: {}",
                        rm.topic(),
                        rm.partition(),
                        rm.offset()
                );

                log.info("Difference in time: {}", rm.timestamp() - currentTimeMillis);
                log.info("Difference in time: {}", rm.timestamp() - nowMs);

                // flush data - synchronous
                producer.flush();
            } catch (Exception e) {
                log.error("Unable to send the message: {}", e.getMessage());
                producer.close();
            }

            // Sleep 1 second
            Thread.sleep(1000);
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
