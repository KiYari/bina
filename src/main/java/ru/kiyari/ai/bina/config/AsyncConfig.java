package ru.kiyari.ai.bina.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Configuration
public class AsyncConfig {
    @Bean
    public ExecutorService fixedThreadPool() {
        // Альтернативно: фиксированный пул потоков
        return Executors.newFixedThreadPool(24);
    }
}
