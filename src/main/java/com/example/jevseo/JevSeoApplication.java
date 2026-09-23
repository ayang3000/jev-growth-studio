package com.example.jevseo;

import com.example.jevseo.config.AiPlatformProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(AiPlatformProperties.class)
public class JevSeoApplication {

    public static void main(String[] args) {
        SpringApplication.run(JevSeoApplication.class, args);
    }
}
