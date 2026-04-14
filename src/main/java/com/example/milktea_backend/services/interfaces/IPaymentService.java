package com.example.milktea_backend.services.interfaces;

import com.example.milktea_backend.dtos.requests.WebhookRequest;

public interface IPaymentService {
    void processWebhook(WebhookRequest webhookRequest);
}
