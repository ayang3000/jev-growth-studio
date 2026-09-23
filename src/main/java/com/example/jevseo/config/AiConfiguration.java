package com.example.jevseo.config;

import com.example.jevseo.ai.ContentGenerator;
import com.example.jevseo.ai.SpringAiContentGenerator;
import com.example.jevseo.ai.TemplateContentGenerator;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AiConfiguration {

    @Bean
    ContentGenerator contentGenerator(
            ObjectProvider<ChatClient.Builder> builderProvider,
            AiPlatformProperties properties) {
        if (!properties.generator().enabled()) {
            return new TemplateContentGenerator();
        }
        ChatClient.Builder builder = builderProvider.getIfAvailable();
        if (builder != null) {
            return new SpringAiContentGenerator(builder.build());
        }
        return new TemplateContentGenerator();
    }
}
