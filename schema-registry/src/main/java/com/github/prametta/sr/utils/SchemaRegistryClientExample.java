package com.github.prametta.sr.utils;

import io.confluent.kafka.schemaregistry.client.CachedSchemaRegistryClient;
import io.confluent.kafka.schemaregistry.client.SchemaRegistryClient;
import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;

@Log4j2
public class SchemaRegistryClientExample {

    @SneakyThrows
    public static void main(String[] args) {
        SchemaRegistryClient schemaRegistryClient = new CachedSchemaRegistryClient("http://localhost:8081", 100);
        var subjects = schemaRegistryClient.getAllSubjects();
        log.info("subjects: {}", subjects);
        var subjectMetadata = schemaRegistryClient.getLatestSchemaMetadata("beers-value");
        log.info("subjectMetadata: {}", subjectMetadata.getMetadata());
        var schema = schemaRegistryClient.getSchemas("beers-value", false, false);
        log.info("schema: {}", schema);
    }
}
