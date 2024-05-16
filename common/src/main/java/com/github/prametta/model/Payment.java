package com.github.prametta.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import java.io.Serializable;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class Payment implements Serializable {

    @JsonProperty
    public String id;

    @JsonProperty
    public String name;

    @JsonProperty
    public String cardNumber;

    @JsonProperty
    public String amount;

    @JsonProperty
    public String currency;

    @JsonProperty
    public String date;

    @JsonProperty
    public String cvv;

    @JsonProperty
    public String type;

    @JsonProperty
    public String status;
}
