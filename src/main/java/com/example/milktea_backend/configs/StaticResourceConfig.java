package com.example.milktea_backend.configs;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;

@Configuration
public class StaticResourceConfig implements WebMvcConfigurer {

    @Value("${app.media.upload-dir:uploads}")
    private String uploadDir;

    @Value("${app.media.public-path:/uploads}")
    private String publicPath;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String normalizedPublicPath = publicPath.startsWith("/") ? publicPath : "/" + publicPath;
        String location = Path.of(uploadDir).toAbsolutePath().normalize().toUri().toString();

        registry.addResourceHandler(normalizedPublicPath + "/**")
                .addResourceLocations(location);
    }
}
