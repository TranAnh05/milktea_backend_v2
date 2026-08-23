package com.example.milktea_backend.dtos.requests;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class ResendEmailRequest {
    private String from;
    private List<String> to;
    private String subject;
    private String html;
}
