package com.example.milktea_backend.dtos.requests;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class WebhookRequest {
    @JsonProperty("gateway")
    private String gateway;

    @JsonProperty("referenceCode")
    private String transactionNo;

    @JsonProperty("transferAmount")
    private Integer amountIn;

    @JsonProperty("content")
    private String content;

    private String signature;
}
