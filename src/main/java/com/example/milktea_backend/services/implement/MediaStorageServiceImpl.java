package com.example.milktea_backend.services.implement;

import com.example.milktea_backend.services.interfaces.IMediaStorageService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.net.URI;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import java.util.UUID;

@Service
public class MediaStorageServiceImpl implements IMediaStorageService {

    @Value("${app.media.upload-dir:uploads}")
    private String uploadDir;

    @Value("${app.media.public-path:/uploads}")
    private String publicPath;

    @Override
    public String persistExternalImage(String rawValue, String prefix) {
        if (rawValue == null || rawValue.isBlank()) return rawValue;

        String value = rawValue.trim();
        if (!isRemoteHttpUrl(value)) return value;

        try {
            URI uri = URI.create(value);
            URLConnection connection = uri.toURL().openConnection();
            connection.setConnectTimeout(8000);
            connection.setReadTimeout(15000);

            String contentType = connection.getContentType();
            if (contentType == null || !contentType.toLowerCase(Locale.ROOT).startsWith("image/")) {
                return value;
            }

            String ext = extensionFromContentType(contentType);
            if (ext == null) ext = extensionFromUrlPath(uri.getPath());
            if (ext == null) ext = "jpg";

            Path dir = Path.of(uploadDir).toAbsolutePath().normalize();
            Files.createDirectories(dir);

            String safePrefix = (prefix == null || prefix.isBlank()) ? "image" : prefix.replaceAll("[^a-zA-Z0-9_-]", "").toLowerCase(Locale.ROOT);
            String fileName = safePrefix + "-" + UUID.randomUUID() + "." + ext;
            Path targetFile = dir.resolve(fileName);

            try (InputStream in = connection.getInputStream()) {
                Files.copy(in, targetFile, StandardCopyOption.REPLACE_EXISTING);
            }

            String normalizedPublicPath = publicPath.startsWith("/") ? publicPath : "/" + publicPath;
            return normalizedPublicPath + "/" + fileName;
        } catch (Exception ex) {
            return value;
        }
    }

    @Override
    public String persistUploadedImage(MultipartFile file, String prefix) {
        if (file == null || file.isEmpty()) return null;

        try {
            String contentType = file.getContentType();
            if (contentType == null || !contentType.toLowerCase(Locale.ROOT).startsWith("image/")) {
                return null;
            }

            String ext = extensionFromContentType(contentType);
            if (ext == null) ext = extensionFromFileName(file.getOriginalFilename());
            if (ext == null) ext = "jpg";

            Path dir = Path.of(uploadDir).toAbsolutePath().normalize();
            Files.createDirectories(dir);

            String safePrefix = (prefix == null || prefix.isBlank()) ? "image" : prefix.replaceAll("[^a-zA-Z0-9_-]", "").toLowerCase(Locale.ROOT);
            String fileName = safePrefix + "-" + UUID.randomUUID() + "." + ext;
            Path targetFile = dir.resolve(fileName);

            try (InputStream in = file.getInputStream()) {
                Files.copy(in, targetFile, StandardCopyOption.REPLACE_EXISTING);
            }

            String normalizedPublicPath = publicPath.startsWith("/") ? publicPath : "/" + publicPath;
            return normalizedPublicPath + "/" + fileName;
        } catch (Exception ex) {
            return null;
        }
    }

    private boolean isRemoteHttpUrl(String value) {
        String lower = value.toLowerCase(Locale.ROOT);
        return lower.startsWith("http://") || lower.startsWith("https://");
    }

    private String extensionFromContentType(String contentType) {
        String lower = contentType.toLowerCase(Locale.ROOT);
        if (lower.contains("image/jpeg") || lower.contains("image/jpg")) return "jpg";
        if (lower.contains("image/png")) return "png";
        if (lower.contains("image/webp")) return "webp";
        if (lower.contains("image/gif")) return "gif";
        if (lower.contains("image/svg")) return "svg";
        return null;
    }

    private String extensionFromUrlPath(String path) {
        if (path == null) return null;
        int dot = path.lastIndexOf('.');
        if (dot < 0 || dot == path.length() - 1) return null;
        String ext = path.substring(dot + 1).toLowerCase(Locale.ROOT);
        if (ext.matches("[a-z0-9]{2,5}")) return ext;
        return null;
    }

    private String extensionFromFileName(String fileName) {
        return extensionFromUrlPath(fileName);
    }
}
