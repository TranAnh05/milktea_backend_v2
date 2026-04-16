package com.example.milktea_backend.services.interfaces;

import org.springframework.web.multipart.MultipartFile;

public interface IMediaStorageService {
    String persistExternalImage(String rawValue, String prefix);
    String persistUploadedImage(MultipartFile file, String prefix);
}
