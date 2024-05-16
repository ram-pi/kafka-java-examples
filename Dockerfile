FROM azul/zulu-openjdk:17

COPY ./producer/target/producer-1.0-SNAPSHOT-shaded.jar app.jar
COPY ./jmx_prometheus_javaagent-0.20.0.jar jmx_prometheus_javaagent-0.20.0.jar
COPY ./opentelemetry-javaagent.jar opentelemetry-javaagent.jar
COPY ./prometheus.javaagent.config.yaml prometheus.javaagent.config.yaml
COPY ./client.properties client.properties

CMD ["java", "-javaagent:./jmx_prometheus_javaagent-0.18.0.jar=9999:prometheus.javaagent.config.yaml", "-cp", "app.jar", "io.confluent.prametta.producer.ThroughputProducer"]