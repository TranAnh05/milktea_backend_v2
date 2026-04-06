package com.example.milktea_backend.services.interfaces;

import com.example.milktea_backend.dtos.requests.LoginRequest;
import com.example.milktea_backend.dtos.responses.AuthResponse;

public interface IAuthService {
    AuthResponse login(LoginRequest request);
}
