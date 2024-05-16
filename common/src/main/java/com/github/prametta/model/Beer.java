package com.github.prametta.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.confluent.kafka.schemaregistry.annotations.Schema;
import lombok.*;

import java.io.Serializable;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@ToString
@Schema(value = "{"
        + "\"$schema\": \"http://json-schema.org/draft-07/schema#\","
        + "\"title\": \"Beer\","
        + "\"type\":\"object\","
        + "\"properties\": {\"hop\":{\"type\":\"string\"},\"malt\":{\"type\":\"string\"},\"name\":{\"type\":\"string\"},\"style\":{\"type\":\"string\"},\"yeast\":{\"type\":\"string\"}},"
        + "\"additionalProperties\":false}", refs = {})
public class Beer implements Serializable {

    @JsonProperty
    public String hop;

    @JsonProperty
    public String malt;

    @JsonProperty
    public String name;

    @JsonProperty
    public String style;

    @JsonProperty
    public String yeast;
}
