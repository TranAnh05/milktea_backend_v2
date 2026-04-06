package com.example.milktea_backend.services.interfaces;

public interface IEmailService {
    void sendVerificationEmail(String toEmail, String fullName, String token);
}
