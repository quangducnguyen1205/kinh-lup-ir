package vn.edu.hust.kinhlup.api;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfiguration implements WebMvcConfigurer {
    private final String[] origins;

    public WebConfiguration(@Value("${kinhlup.cors.allowed-origins:http://localhost:5173}") String[] origins) {
        this.origins = origins;
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/search").allowedOrigins(origins).allowedMethods("GET");
        registry.addMapping("/api/documents/**").allowedOrigins(origins).allowedMethods("GET");
    }
}
