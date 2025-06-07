package ru.kiyari.ai.bina.config;

import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.openai.internal.OpenAiClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Configuration
public class ConnectionConfig {
    @Value(value = "${api.key}")
    String apiKey;

    @Bean
    public OpenAiChatModel openAiChatModel() {
        return OpenAiChatModel.builder()
                .apiKey(apiKey)
                .baseUrl("https://hubai.loe.gg/v1")
                .modelName("deepseek-chat")
                .timeout(Duration.ofSeconds(30))
                .maxTokens(1024)
                .build();
    }

    @Bean
    public OpenAiClient openAiClient() {
        return OpenAiClient.builder()
                .apiKey(apiKey)
                .baseUrl("https://hubai.loe.gg/v1")
                .connectTimeout(Duration.ofSeconds(15))
                .build();
    }
}
